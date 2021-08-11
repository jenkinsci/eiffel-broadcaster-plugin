/**
 The MIT License

 Copyright 2021 Axis Communications AB.

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

package com.axis.jenkins.plugins.eiffel.eiffelbroadcaster.pipeline;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURLBuilder;
import hudson.AbortException;
import hudson.Extension;
import hudson.Util;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class CreatePackageURLStep extends Step {
    private @CheckForNull String name;

    private @CheckForNull String namespace;

    private @Nonnull Map<String, String> qualifiers = new HashMap<>();

    private @CheckForNull String subpath;

    private @CheckForNull String type;

    private @CheckForNull String version;

    @DataBoundConstructor
    public CreatePackageURLStep() {
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(this, stepContext);
    }

    public @CheckForNull String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(@CheckForNull String name) {
        this.name = Util.fixEmptyAndTrim(name);
    }

    public @CheckForNull String getNamespace() {
        return namespace;
    }

    @DataBoundSetter
    public void setNamespace(@CheckForNull String namespace) {
        this.namespace = Util.fixEmptyAndTrim(namespace);
    }

    public @Nonnull Map<String, String> getQualifiers() {
        return qualifiers;
    }

    @DataBoundSetter
    public void setQualifiers(@Nonnull Map<String, String> qualifiers) {
        this.qualifiers.clear();
        this.qualifiers.putAll(qualifiers);
    }

    public @CheckForNull String getSubpath() {
        return subpath;
    }

    @DataBoundSetter
    public void setSubpath(@CheckForNull String subpath) {
        this.subpath = Util.fixEmptyAndTrim(subpath);
    }

    public @CheckForNull String getType() {
        return type;
    }

    @DataBoundSetter
    public void setType(@CheckForNull String type) {
        this.type = Util.fixEmptyAndTrim(type);
    }

    public @CheckForNull String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(@CheckForNull String version) {
        this.version = Util.fixEmptyAndTrim(version);
    }

    private static class Execution extends SynchronousStepExecution<String> {
        private static final long serialVersionUID = 1L;
        private final transient CreatePackageURLStep step;

        public Execution(@Nonnull CreatePackageURLStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected String run() throws Exception {
            PackageURLBuilder purlBuilder = PackageURLBuilder.aPackageURL()
                    .withType(step.getType())
                    .withNamespace(step.getNamespace())
                    .withName(step.getName())
                    .withVersion(step.getVersion())
                    .withSubpath(step.getSubpath());
            for (Map.Entry<String, String> entry : step.getQualifiers().entrySet()) {
                purlBuilder.withQualifier(entry.getKey(), entry.getValue());
            }
            try {
                return purlBuilder.build().toString();
            } catch (MalformedPackageURLException e) {
                throw new AbortException(
                        "Error creating package URL (see specification at https://github.com/package-url/purl-spec): " +
                                e.getMessage());
            }
        }
    }

    @Extension
    public static class Descriptor extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Construct a package URL and return it as a string";
        }

        @Override
        public String getFunctionName() {
            return "createPackageURL";
        }
    }
}
