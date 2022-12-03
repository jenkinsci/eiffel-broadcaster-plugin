/**
 The MIT License

 Copyright 2021 Axis Communications AB.
 Copyright 2014 Jesse Glick.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster;

import com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.model.Run;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.util.TimeDuration;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;

/**
 * An {@link Action} that attaches an additional API endpoint to jobs for starting a build with one
 * or more Eiffel links. The endpoint is $JOB_URL/eiffel/build and should behave as the regular
 * $JOB_URL/build endpoint, i.e. you can supply parameters by posting a form with a JSON string.
 * Additionally, it requires a (possibly empty) list of Eiffel links. Example (payload lacking URL
 * encoding to improve readability):
 * <pre>
 * POST $JOB_URL/eiffel/build
 * Content-Type: application/x-www-form-urlencoded
 *
 * json={"eiffellinks": [{"target": "662b3813-bef4-4588-bf75-ffaead24a6d5", "type": "CAUSE"}], "parameter": [{"name": "PARAM_NAME", "value": "param value"}]}
 * </pre>
 * The Eiffel links, if any, will be passed to the build as an {@link EiffelCause} cause.
 * That cause will be used when piecing together the
 * {@link com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.eiffel.EiffelActivityTriggeredEvent}
 * which gets captured in the {@link Run}'s {@link EiffelActivityAction}.
 */
public class BuildWithEiffelLinksAction<
        JobT extends Job<JobT, RunT> & ParameterizedJobMixIn.ParameterizedJob<JobT, RunT>,
        RunT extends Run<JobT, RunT> & Queue.Executable>
        implements Action {
    /** The form parameter that holds the Eiffel links. */
    public static final String FORM_PARAM_EIFFELLINKS = "eiffellinks";

    /** The form parameter that holds the build parameters. */
    public static final String FORM_PARAM_PARAMETERS = "parameter";

    /**
     * The immediate suffix to the URL of the {@link Job}, i.e. the URLs served by this action will have
     * the form $JOB_URL/$URL_SUFFIX.
     */
    public static final String URL_SUFFIX = "eiffel";

    private final JobT job;

    public BuildWithEiffelLinksAction(JobT job) {
        this.job = job;
    }

    /**
     * Responds to a /build request by parsing the posted form and transforming the Eiffel links
     * and (optionally) build parameters into actions that'll get passed to the new build.
     */
    @RequirePOST
    public void doBuild(StaplerRequest req, StaplerResponse rsp, @QueryParameter TimeDuration delay)
            throws IOException {
        job.checkPermission(Item.BUILD);

        if (!job.isBuildable()) {
            throw HttpResponses.error(SC_CONFLICT, new IOException(job.getFullName() + " is not buildable"));
        }

        if (delay == null) {
            delay = new TimeDuration(TimeUnit.MILLISECONDS.convert(job.getQuietPeriod(), TimeUnit.SECONDS));
        }

        List<Action> actions = new ArrayList<>();
        JSONObject formData;
        try {
            formData = req.getSubmittedForm();
        }
        catch (ServletException e) {
            throw HttpResponses.error(SC_BAD_REQUEST,
                    new IllegalArgumentException(
                            "Missing or invalid contents of \"json\" form field: " + e.toString(), e));
        }
        try {
            ParametersDefinitionProperty pp = job.getProperty(ParametersDefinitionProperty.class);
            if (pp != null) {
                Action paramAction = getParametersAction(req, formData, pp);
                if (paramAction != null) {
                    actions.add(paramAction);
                }
            }

            List<Cause> causes = new ArrayList<>(Arrays.asList(getCallerCause(req)));
            EiffelCause eiffelCause = getEiffelCause(formData);
            if (eiffelCause != null) {
                causes.add(eiffelCause);
            }
            actions.add(new CauseAction(causes));
        }
        catch (IllegalArgumentException e) {
            throw HttpResponses.error(SC_BAD_REQUEST, e);
        }

        Queue.Item queuedBuild = Jenkins.get().getQueue().schedule2(job, delay.getTimeInSeconds(), actions).getItem();
        if (queuedBuild != null) {
            rsp.sendRedirect(SC_CREATED, req.getContextPath() + '/' + queuedBuild.getUrl());
        } else {
            rsp.sendRedirect(".");
        }
    }

    /**
     * Returns a {@link Cause} that indicates the caller that requested the build (either a
     * {@link Cause.RemoteCause} or a {@link Cause.UserIdCause}).
     * <p>
     * The implementation was copied from
     * {@link ParameterizedJobMixIn#getBuildCause(ParameterizedJobMixIn.ParameterizedJob, StaplerRequest)}
     * and only slightly modified.
     */
    public Cause getCallerCause(StaplerRequest req) {
        @SuppressWarnings("deprecation")
        hudson.model.BuildAuthorizationToken authToken = job.getAuthToken();
        if (authToken != null && authToken.getToken() != null && req.getParameter("token") != null) {
            // Optional additional cause text when starting via token
            String causeText = req.getParameter("cause");
            return new Cause.RemoteCause(req.getRemoteAddr(), causeText);
        } else {
            return new Cause.UserIdCause();
        }
    }

    /**
     * Parses the posted form and transforms the supplied list of Eiffel event links
     * into an {@link EiffelCause}. The JSON object in the form value must have a
     * {@link #FORM_PARAM_EIFFELLINKS} key that contains the array of Eiffel links.
     * The array may be empty in which case null is returned.
     */
    @CheckForNull
    private EiffelCause getEiffelCause(final JSONObject formData) {
        // This mixing of different parsed JSON representation isn't great, but the StaplerRequest
        // provides us a JSONObject and we want to use Jackson for the events themselves.
        try {
            JSONArray eiffelLinks = formData.getJSONArray(FORM_PARAM_EIFFELLINKS);
            ObjectMapper mapper = new ObjectMapper();
            CollectionType listType = mapper.getTypeFactory().constructCollectionType(
                    List.class, EiffelEvent.Link.class);
            List<EiffelEvent.Link> links = mapper.readValue(eiffelLinks.toString(), listType);
            return links.isEmpty() ? null : new EiffelCause(links);
        }
        catch (JSONException e) {
            throw new IllegalArgumentException(String.format(
                    "URL parameter '%s' did not contain a JSON array", FORM_PARAM_EIFFELLINKS), e);
        }
        catch (JsonProcessingException e) {
            throw new IllegalArgumentException(String.format(
                    "URL parameter '%s' could not be deserialized into a list of Eiffel links: %s",
                    FORM_PARAM_EIFFELLINKS, e.toString()), e);
        }
    }

    /**
     * Attempts to parse the posted form and transform the given build parameters and their
     * values into a {@link ParametersAction}. The JSON object in the form value may have a
     * {@link #FORM_PARAM_PARAMETERS} key that contains another object with the desired
     * build parameters. If the {@link #FORM_PARAM_PARAMETERS} key is missing the default
     * values of the parameters will be used. Returns null if no parameters were supplied.
     */
    @CheckForNull
    private ParametersAction getParametersAction(final StaplerRequest req,
                                                 final JSONObject formData,
                                                 final ParametersDefinitionProperty pp) {
        List<ParameterValue> values = new ArrayList<>();

        // Collect parameters given in this request.
        List<JSONObject> givenParams = new ArrayList<>();
        try {
            Object inputParams = formData.get(FORM_PARAM_PARAMETERS);
            if (inputParams != null) {
                for (Object paramObject : JSONArray.fromObject(inputParams)) {
                    givenParams.add((JSONObject) paramObject);
                }
            }
        }
        catch (ClassCastException | JSONException e) {
            throw new IllegalArgumentException(String.format(
                    "URL parameter '%s' couldn't be deserialized to a parameter list: %s",
                    FORM_PARAM_PARAMETERS, e.toString()), e);
        }

        // Did the request include any parameters that haven't been defined for this job?
        Set<String> missingParamDefs = givenParams.stream()
                .map(p -> p.getString("name"))
                .filter(n -> pp.getParameterDefinition(n) == null)
                .collect(Collectors.toSet());
        if (!missingParamDefs.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Build request provided values for the following parameters that aren't defined in the job: %s'",
                    missingParamDefs));
        }

        // Add parameter values for all given parameters.
        for (JSONObject param : givenParams) {
            String name = param.getString("name");
            ParameterDefinition parameterDef = pp.getParameterDefinition(name);
            if (parameterDef == null) {
                // We've already checked that all given parameters have definitions in this job,
                // but if the job definition changes after that check was done we're off to the races.
                continue;
            }
            ParameterValue parameterValue = parameterDef.createValue(req, param);
            if (parameterValue == null) {
                throw new IllegalArgumentException(String.format(
                        "Cannot initialize the '%s' parameter with the given value", name));
            }
            values.add(parameterValue);
        }

        // Add default parameter values for parameters that haven't been given values above.
        Set<String> paramsWithValues = values.stream()
                .map(ParameterValue::getName)
                .collect(Collectors.toSet());
        values.addAll(pp.getParameterDefinitions().stream()
                .filter(pd -> !paramsWithValues.contains(pd.getName()))
                .map(ParameterDefinition::getDefaultParameterValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        return values.isEmpty() ? null : new ParametersAction(values);
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return URL_SUFFIX;
    }
}
