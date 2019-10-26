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

import java.util.List;

public class ShrinkerExtension {
    public boolean inlineR = true;

    public boolean skipDebugInlineR = false;

    /**
     * 使用类似skipPkgs=["com.baidu.swan","com.baidu.searchbox"]这样
     */
    public List<String> skipRPkgs;

    public boolean isInlineR() {
        return inlineR;
    }

    public void setInlineR(boolean inlineR) {
        this.inlineR = inlineR;
    }

    public boolean isSkipDebugInlineR() {
        return skipDebugInlineR;
    }

    public void setSkipDebugInlineR(boolean skipDebugInlineR) {
        this.skipDebugInlineR = skipDebugInlineR;
    }

    public List<String> getSkipRPkgs() {
        return skipRPkgs;
    }

    public void setSkipRPkgs(List<String> skipRPkgs) {
        this.skipRPkgs = skipRPkgs;
    }
}