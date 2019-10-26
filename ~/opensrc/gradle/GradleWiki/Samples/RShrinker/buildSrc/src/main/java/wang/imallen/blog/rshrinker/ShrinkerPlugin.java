/*
 * Copyright (c) 2017 Yrom Wang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wang.imallen.blog.rshrinker;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import wang.imallen.blog.rshrinker.log.Logger;

import org.gradle.api.Plugin;
import org.gradle.api.Project;


public class ShrinkerPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Logger.d("ShrinkerPlugin.apply()");
        if (!project.getPlugins().hasPlugin(AppPlugin.class)) {
            throw new UnsupportedOperationException("Plugin 'shrinker' can only apply with 'com.android.application'");
        }
        AppExtension android = project.getExtensions().getByType(AppExtension.class);
        ShrinkerExtension config = project.getExtensions().create("rshrink", ShrinkerExtension.class);
        android.registerTransform(new InlineRTransform(config));

        project.afterEvaluate(it -> {
                    InlineContext.parseShrinkerExtension();
                }
        );

    }
}