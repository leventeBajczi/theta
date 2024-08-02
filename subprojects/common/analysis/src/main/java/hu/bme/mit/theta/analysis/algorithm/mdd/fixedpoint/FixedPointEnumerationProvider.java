/*
 *  Copyright 2024 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hu.bme.mit.theta.analysis.algorithm.mdd.fixedpoint;

import hu.bme.mit.delta.java.mdd.*;
import hu.bme.mit.theta.analysis.algorithm.mdd.ansd.AbstractNextStateDescriptor;

public interface FixedPointEnumerationProvider extends MddTransformationProvider<AbstractNextStateDescriptor> {
    MddHandle compute(
            AbstractNextStateDescriptor.Postcondition initializer,
            AbstractNextStateDescriptor nextStateRelation,
            MddVariableHandle highestAffectedVariable
    );

    Cache getCache();

    CacheManager<?> getCacheManager();
}
