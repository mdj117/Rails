/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rails.carts.utils;

import org.terasology.math.Vector3i;

import javax.vecmath.Vector3f;

public class MinecartHelper {
    public static void setVectorToDirection(Vector3f in, Vector3f direction) {
        in.x *= Math.signum(direction.x);
        in.y *= Math.signum(direction.y);
        in.z *= Math.signum(direction.z);
    }

    public static void setVectorToDirection(Vector3f in, float directionX, float directionY, float directionZ) {
        in.x *= Math.signum(directionX);
        in.y *= Math.signum(directionY);
        in.z *= Math.signum(directionZ);
    }
}
