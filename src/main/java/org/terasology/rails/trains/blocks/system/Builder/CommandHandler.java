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
package org.terasology.rails.trains.blocks.system.Builder;

import com.bulletphysics.linearmath.QuaternionUtil;
import com.google.common.collect.Lists;
import org.lwjgl.util.vector.Quaternion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.AABB;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.physics.CollisionGroup;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.rails.trains.blocks.components.TrainRailComponent;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;
import org.terasology.rails.trains.blocks.system.Railway;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.utilities.Assets;

import java.util.List;

/**
 * Created by adeon on 09.09.14.
 */
public class CommandHandler {
    private EntityManager entityManager;
    private Physics physics;
    private final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private static CommandHandler instance = null;

    public static CommandHandler getInstance() {
        if (instance == null) {
            instance = new CommandHandler();
        }

        return instance;
    }

    private CommandHandler() {
        this.entityManager = CoreRegistry.get(EntityManager.class);
        this.physics = CoreRegistry.get(Physics.class);
    }

    public TaskResult run(List<Command> commands, EntityRef selectedTrack, boolean preview) {
        EntityRef firstTrack = EntityRef.NULL;
        for( Command command : commands ) {
            if (command.build) {
                selectedTrack = buildTrack(selectedTrack, command, preview);
                if (selectedTrack.equals(EntityRef.NULL)) {
                    return new TaskResult(firstTrack, null, false);
                }
                if (firstTrack.equals(EntityRef.NULL)) {
                    firstTrack = selectedTrack;
                }
            } else {
                boolean removeResult = false;
                if (!removeResult) {
                    return new TaskResult(EntityRef.NULL, EntityRef.NULL, false);
                }
            }
        }
        return new TaskResult(firstTrack, selectedTrack, true);
    }

    private EntityRef buildTrack(EntityRef selectedTrack, Command command, boolean preview) {

        Orientation newOrientation = new Orientation(0,0,0);
        Orientation fixOrientation = null;
        Vector3f newPosition;
        Vector3f prevPosition = command.checkedPosition;
        boolean newTrack = false;
        boolean findedNearestRail = false;
        float startYaw = 0;
        float startPitch = 0;
        if (selectedTrack.equals(EntityRef.NULL)) {
            AABB aabb = AABB.createCenterExtent(prevPosition,
                    new Vector3f(1.5f, 0.5f, 1.5f)
            );

            List<EntityRef> aroundList = physics.scanArea(aabb, StandardCollisionGroup.WORLD, StandardCollisionGroup.KINEMATIC);
            if (!aroundList.isEmpty()) {
                for (EntityRef checkEntity : aroundList) {
                    if (checkEntity.hasComponent(TrainRailComponent.class)) {
                        TrainRailComponent trainRailComponent = checkEntity.getComponent(TrainRailComponent.class);
                        if (Math.abs(trainRailComponent.yaw - command.orientation.yaw) <= 7.5 &&
                            Math.abs(trainRailComponent.pitch - command.orientation.pitch) <= 30 &&
                            trainRailComponent.linkedTracks.size() < 2
                           ) {
                            selectedTrack = checkEntity;
                            findedNearestRail = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!selectedTrack.equals(EntityRef.NULL)) {
            TrainRailComponent trainRailComponent = selectedTrack.getComponent(TrainRailComponent.class);
            if (trainRailComponent.chunkKey.equals(command.chunkKey)) {
                prevPosition = new Vector3f(trainRailComponent.endPosition);
                startYaw = trainRailComponent.yaw;
                startPitch = trainRailComponent.pitch;
            } else {
                if (findedNearestRail) {
                    float firstSide, secondSide;
                    Vector3f diff = new Vector3f(prevPosition);
                    diff.sub(trainRailComponent.startPosition);
                    firstSide = diff.lengthSquared();
                    diff.set(prevPosition);
                    diff.sub(trainRailComponent.endPosition);
                    secondSide = diff.lengthSquared();

                    if (firstSide > secondSide) {
                        prevPosition = new Vector3f(trainRailComponent.endPosition);
                    } else {
                        prevPosition = new Vector3f(trainRailComponent.startPosition);
                    }
                } else {
                    if (trainRailComponent.linkedTracks.size() > 0) {
                        EntityRef linkedTrack =  trainRailComponent.linkedTracks.get(0);
                        LocationComponent locationComponent = linkedTrack.getComponent(LocationComponent.class);
                        prevPosition = locationComponent.getWorldPosition();
                        float firstSide, secondSide;
                        Vector3f diff = new Vector3f(prevPosition);
                        diff.sub(trainRailComponent.startPosition);
                        firstSide = diff.lengthSquared();
                        diff.set(prevPosition);
                        diff.sub(trainRailComponent.endPosition);
                        secondSide = diff.lengthSquared();

                        if (firstSide < secondSide) {
                            prevPosition = new Vector3f(trainRailComponent.endPosition);
                        } else {
                            prevPosition = new Vector3f(trainRailComponent.startPosition);
                        }
                    }
                }
                startPitch = trainRailComponent.pitch;
                newTrack = true;
            }
        } else {
            newTrack = true;
        }

        String prefab = "rails:railBlock";
        if (newTrack && !selectedTrack.equals(EntityRef.NULL)) {
            logger.info("BEFORE:   " + prevPosition);


            prevPosition.y = Math.round(prevPosition.y) - 0.35f;
            logger.info("AFTER1:   " + prevPosition);
            switch ((int)command.orientation.yaw) {
                case 90:
                    prevPosition.x = (float)Math.ceil(prevPosition.x);
                    prevPosition.x -= 0.5f;
                    break;
                case 270:
                    prevPosition.x = (float)Math.floor(prevPosition.x);
                    prevPosition.x += 0.5f;
                    break;
                case 0:
                    prevPosition.z = (float)Math.ceil(prevPosition.z);
                    prevPosition.z -= 0.5f;
                    break;
                case 180:
                    prevPosition.z = (float)Math.floor(prevPosition.z);
                    prevPosition.z += 0.5f;
                    break;
            }
            logger.info("AFTER2:   " + prevPosition);
        }

        switch(command.type) {
            case STRAIGHT:
                newOrientation = new Orientation(startYaw, startPitch, 0);

                if (newTrack) {
                    newOrientation.add(command.orientation);
                }

                if (startPitch != 0) {
                    fixOrientation = new Orientation(270f, 0, 0);
                } else {
                    fixOrientation = new Orientation(90f, 0, 0);
                }
                TrainRailComponent tr = selectedTrack.getComponent(TrainRailComponent.class);

                break;
            case UP:
                float pitch = startPitch + Railway.STANDARD_PITCH_ANGLE_CHANGE;

                if (newTrack) {
                    newOrientation.add(command.orientation);
                }

                if (pitch > Railway.STANDARD_ANGLE_CHANGE * 3) {
                    pitch = Railway.STANDARD_ANGLE_CHANGE * 3;
                    newOrientation.add(new Orientation(startYaw, pitch, 0));
                } else {
                    newOrientation.add(new Orientation(startYaw, startPitch + Railway.STANDARD_PITCH_ANGLE_CHANGE, 0));
                }

                TrainRailComponent trd = selectedTrack.getComponent(TrainRailComponent.class);

                fixOrientation = new Orientation(270f, 0, 0);
                prefab = "rails:railBlock-up";
                break;
            case DOWN:
                if (newTrack) {
                    newOrientation.add(command.orientation);
                }

                newOrientation.add(new Orientation(startYaw, startPitch - Railway.STANDARD_PITCH_ANGLE_CHANGE, 0));

                fixOrientation = new Orientation(270f, 0, 0);
                prefab = "rails:railBlock-down";
                break;
            case LEFT:
                newOrientation = new Orientation(startYaw + Railway.STANDARD_ANGLE_CHANGE, startPitch, 0);
                fixOrientation = new Orientation(90f, 0, 0);
                prefab = "rails:railBlock-left";
                break;
            case RIGHT:
                newOrientation = new Orientation(startYaw - Railway.STANDARD_ANGLE_CHANGE, startPitch, 0);
                fixOrientation = new Orientation(90f, 0, 0);
                prefab = "rails:railBlock-right";
                break;
            case CUSTOM:
                newOrientation = new Orientation(command.orientation.yaw, command.orientation.pitch, command.orientation.roll);
                fixOrientation = new Orientation(90f, 0, 0);
                break;
        }

        newPosition =  Railway.getInstance().calculateEndTrackPosition(newOrientation,prevPosition);
        EntityRef track = createEntityInTheWorld(prefab, command, selectedTrack, newPosition, newOrientation, fixOrientation, preview);
        return track;
    }

    private EntityRef createEntityInTheWorld(String prefab, Command command, EntityRef prevTrack,  Vector3f position, Orientation newOrientation, Orientation fixOrientation, boolean preview) {
        Quat4f yawPitch =  new Quat4f(TeraMath.DEG_TO_RAD * (newOrientation.yaw + fixOrientation.yaw),TeraMath.DEG_TO_RAD * (newOrientation.roll + fixOrientation.roll),TeraMath.DEG_TO_RAD * (newOrientation.pitch + fixOrientation.pitch));


        EntityRef railBlock = entityManager.create(prefab, position);

        AABB aabb = AABB.createCenterExtent(position,
                new Vector3f(0.5f, 0.25f, 0.5f)
        );
        List<EntityRef> aroundList = physics.scanArea(aabb, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD, StandardCollisionGroup.KINEMATIC);
        if (!aroundList.isEmpty()) {
            if (preview) {
                changeMaterial(railBlock, "rails:minecart-unjoin");
                setStaticBody(railBlock);
            } else {
                for (EntityRef checkEntity : aroundList) {
                    if (checkEntity.hasComponent(TrainRailComponent.class)) {
                        TrainRailComponent checkEntityTrainRailComponent = checkEntity.getComponent(TrainRailComponent.class);
                        if (!checkEntity.equals(prevTrack) && !checkEntityTrainRailComponent.chunkKey.equals(command.chunkKey)) {
                                Railway.getInstance().removeChunk(command.chunkKey);
                                railBlock.destroy();
                                return EntityRef.NULL;
                        }
                    }
                }
            }
        } else {
            if (preview) {
                changeMaterial(railBlock, "rails:minecart-join");
                setStaticBody(railBlock);
            }
        }

        LocationComponent locationComponent = railBlock.getComponent(LocationComponent.class);
        locationComponent.setWorldRotation(yawPitch);

        TrainRailComponent trainRailComponent = railBlock.getComponent(TrainRailComponent.class);
        trainRailComponent.pitch = newOrientation.pitch;
        trainRailComponent.yaw = newOrientation.yaw;
        trainRailComponent.roll = newOrientation.roll;
        trainRailComponent.type = command.type;
        trainRailComponent.startPosition = Railway.getInstance().calculateStartTrackPosition(newOrientation, position);
        trainRailComponent.endPosition = Railway.getInstance().calculateEndTrackPosition(newOrientation, position);
        trainRailComponent.chunkKey = command.chunkKey;

        Railway.getInstance().getChunk(command.chunkKey).add(railBlock);

        if (!preview) {
            Vector3f dir = new Vector3f(position);
            dir.sub(trainRailComponent.startPosition);
            dir.normalize();
            Railway.getInstance().createTunnel(position, dir, Railway.getInstance().getChunk(command.chunkKey).size()%2 != 0);
        }

        if (!prevTrack.equals(EntityRef.NULL)&&!preview) {
            trainRailComponent.linkedTracks.add(prevTrack);
            TrainRailComponent prevTrainRailComponent = prevTrack.getComponent(TrainRailComponent.class);
            prevTrainRailComponent.linkedTracks.add(railBlock);
            prevTrack.saveComponent(prevTrainRailComponent);
        }

        railBlock.saveComponent(locationComponent);
        railBlock.saveComponent(trainRailComponent);
        return railBlock;
    }

    private void changeMaterial(EntityRef entity, String material) {
        MeshComponent mesh = entity.getComponent(MeshComponent.class);
        mesh.material = Assets.getMaterial(material).get();
        entity.saveComponent(mesh);
    }

    private void setStaticBody(EntityRef entity) {
        RigidBodyComponent rigidBodyComponent = entity.getComponent(RigidBodyComponent.class);
        rigidBodyComponent.collisionGroup = StandardCollisionGroup.STATIC;
        rigidBodyComponent.collidesWith = Lists.<CollisionGroup>newArrayList();
        entity.saveComponent(rigidBodyComponent);
    }
}