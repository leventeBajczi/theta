/*
 *  Copyright 2023 Budapest University of Technology and Economics
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
package hu.bme.mit.theta.common;

import com.google.common.collect.ImmutableList;

public final class Tuple4<T1, T2, T3, T4> extends Tuple {

    private Tuple4(final T1 e1, final T2 e2, final T3 e3, final T4 e4) {
        super(ImmutableList.of(e1, e2, e3, e4));
    }

    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(final T1 e1, final T2 e2, final T3 e3,
                                                             final T4 e4) {
        return new Tuple4<>(e1, e2, e3, e4);
    }

    public T1 get1() {
        @SuppressWarnings("unchecked") final T1 result = (T1) elem(0);
        return result;
    }

    public T2 get2() {
        @SuppressWarnings("unchecked") final T2 result = (T2) elem(1);
        return result;
    }

    public T3 get3() {
        @SuppressWarnings("unchecked") final T3 result = (T3) elem(2);
        return result;
    }

    public T4 get4() {
        @SuppressWarnings("unchecked") final T4 result = (T4) elem(3);
        return result;
    }

}
