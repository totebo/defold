// Copyright 2020 The Defold Foundation
// Licensed under the Defold License version 1.0 (the "License"); you may not use
// this file except in compliance with the License.
//
// You may obtain a copy of the License, together with FAQs at
// https://www.defold.com/license
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.dynamo.bob.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.List;
import java.security.CodeSource;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Tuple4d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.dynamo.bob.Bob;
import com.dynamo.bob.ClassLoaderScanner;
import com.dynamo.bob.fs.IResource;
import com.dynamo.bob.fs.ZipMountPoint;
import com.dynamo.bob.textureset.TextureSetGenerator.UVTransform;
import com.dynamo.bob.util.RigUtil;
import com.dynamo.bob.util.RigUtil.BaseSlot;
import com.dynamo.bob.util.RigUtil.SlotAnimationTrack;
import com.dynamo.bob.util.RigUtil.Animation;
import com.dynamo.bob.util.RigUtil.AnimationCurve;
import com.dynamo.bob.util.RigUtil.AnimationTrack;
import com.dynamo.bob.util.RigUtil.AnimationTrack.Property;
import com.dynamo.bob.util.RigUtil.Bone;
import com.dynamo.bob.util.RigUtil.EventTrack;
import com.dynamo.bob.util.RigUtil.MeshAttachment;
import com.dynamo.bob.util.RigUtil.Transform;
import com.dynamo.bob.util.RigUtil.UVTransformProvider;
import com.dynamo.rig.proto.Rig;
import com.dynamo.rig.proto.Rig.MeshAnimationTrack;

public class SpineSceneTest {
    private static final double EPSILON = 0.000001;
    private ZipMountPoint mp;
    private ClassLoaderScanner scanner = null;

    public SpineSceneTest() throws IOException {
        try {
            CodeSource src = getClass().getProtectionDomain().getCodeSource();
            String jarPath = new File(src.getLocation().toURI()).getAbsolutePath();
            this.mp = new ZipMountPoint(null, jarPath, false);
            this.mp.mount();
        } catch (Exception e) {
            // let the tests fail later on
            System.err.printf("Failed mount the .jar file");
        }

        String jar = Bob.getJarFile("fmt-spine.jar");
        this.scanner = new ClassLoaderScanner();
        this.scanner.addUrl(new File(jar));
    }

    private Class<?> getClass(String className) {
        try {
            return Class.forName(className, true, scanner.getClassLoader());
        } catch(ClassNotFoundException e) {
            System.err.printf("Class not found '%s':\n", className);
            return null;
        }
    }

    private static class TestUVTProvider implements UVTransformProvider {
        @Override
        public UVTransform getUVTransform(String animId) {
            return new UVTransform(new Point2d(0.0, 0.0), new Vector2d(0.5, 1.0), false);
        }
    }

    private static void assertTuple3(double x, double y, double z, Tuple3d t) {
        assertEquals(x, t.x, EPSILON);
        assertEquals(y, t.y, EPSILON);
        assertEquals(z, t.z, EPSILON);
    }

    private static void assertTuple3(Tuple3d expected, Tuple3d actual) {
        assertTuple3(expected.x, expected.y, expected.z, actual);
    }

    private static void assertTuple4(double x, double y, double z, double w, Tuple4d t) {
        assertEquals(x, t.x, EPSILON);
        assertEquals(y, t.y, EPSILON);
        assertEquals(z, t.z, EPSILON);
        assertEquals(w, t.w, EPSILON);
    }

    private static void assertTuple4(Tuple4d expected, Tuple4d actual) {
        assertTuple4(expected.x, expected.y, expected.z, expected.w, actual);
    }

    private static void assertTransform(Transform t, Point3d position, Quat4d rotation, Vector3d scale) {
        if (position != null) {
            assertTuple3(position, t.position);
        }
        if (rotation != null) {
            assertTuple4(rotation, t.rotation);
        }
        if (scale != null) {
            assertTuple3(scale, t.scale);
        }
    }

    private static void assertFloatArrays(float[] expected, float[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i) {
            assertEquals(expected[i], actual[i], EPSILON);
        }
    }

    private static void assertIntArrays(int[] expected, int[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i) {
            assertEquals(expected[i], actual[i]);
        }
    }

    private static void assertPoint3dEquals(Point3d v0, Point3d v1, double delta) {
        assertEquals(v0.getX(), v1.getX(), delta);
        assertEquals(v0.getY(), v1.getY(), delta);
        assertEquals(v0.getZ(), v1.getZ(), delta);
    }

    private static void assertQuat4dEquals(Quat4d v0, Quat4d v1, double delta) {
        assertEquals(v0.getX(), v1.getX(), delta);
        assertEquals(v0.getY(), v1.getY(), delta);
        assertEquals(v0.getZ(), v1.getZ(), delta);
        assertEquals(v0.getW(), v1.getW(), delta);
    }

    @Test
    public void testTransformRot() {
        Transform t = new Transform();
        t.setZAngleDeg(90);
        double halfSq2 = 0.5 * Math.sqrt(2.0);
        assertTuple4(0.0, 0.0, halfSq2, halfSq2, t.rotation);
    }

    @Test
    public void testTransformMul() {
        Transform t1 = new Transform();
        t1.position.set(1.0, 2.0, 3.0);
        t1.setZAngleDeg(90.0);
        t1.scale.set(1.0, 2.0, 3.0);
        Transform t2 = new Transform();
        t2.position.set(2.0, 3.0, 4.0);
        t2.scale.set(2.0, 3.0, 4.0);
        t1.mul(t2);
        assertTuple3(-5.0, 4.0, 15.0, t1.position);
        double halfSq2 = 0.5 * Math.sqrt(2.0);
        assertTuple4(0.0, 0.0, halfSq2, halfSq2, t1.rotation);
        assertTuple3(2.0, 6.0, 12.0, t1.scale);
    }

    @Test
    public void testTransformInv() {
        Transform t = new Transform();
        t.position.set(1.0, 2.0, 3.0);
        t.setZAngleDeg(90.0);
        t.scale.set(1.0, 2.0, 3.0);
        Transform identity = new Transform(t);
        identity.inverse();
        identity.mul(t);
        assertTuple3(0.0, 0.0, 0.0, identity.position);
        assertTuple4(0.0, 0.0, 0.0, 1.0, identity.rotation);
        assertTuple3(1.0, 1.0, 1.0, identity.scale);
    }

    private Object load(String filename) throws IOException {
        IResource resource = this.mp.get("test/" + filename);
        if (resource != null) {
            byte[] data = resource.getContent();

            Object spineSceneUtil;
            try {
                Class<?> spineSceneUtilClass = this.getClass("com.dynamo.bob.pipeline.SpineSceneUtil");
                Method loadJson = spineSceneUtilClass.getDeclaredMethod("loadJson", java.io.InputStream.class, UVTransformProvider.class);
                spineSceneUtil = (Object) loadJson.invoke(null, new ByteArrayInputStream(data), new TestUVTProvider());
            } catch (NoSuchMethodException e) {
                System.err.printf("No method com.dynamo.bob.pipeline.SpineSceneUtil.%s() found!", "loadJson");
                e.printStackTrace(System.err);
                return null;
            } catch (Exception e) {
                e.printStackTrace(System.err);
                return null;
            }

            return spineSceneUtil;
        }
        return null;
    }

    private Object load(InputStream input) throws IOException {
        Object spineSceneUtil;
        try {
            Class<?> spineSceneUtilClass = this.getClass("com.dynamo.bob.pipeline.SpineSceneUtil");
            Method loadJson = spineSceneUtilClass.getDeclaredMethod("loadJson", java.io.InputStream.class, UVTransformProvider.class);
            spineSceneUtil = (Object) loadJson.invoke(null, input, new TestUVTProvider());
        } catch (NoSuchMethodException e) {
            System.err.printf("No method com.dynamo.bob.pipeline.SpineSceneUtil.%s() found!", "loadJson");
            e.printStackTrace(System.err);
            return null;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
        return spineSceneUtil;
    }

    private static Object callFunction(Object instance, String functionName) {
        try {
            Method function = instance.getClass().getDeclaredMethod(functionName);
            return function.invoke(instance);
        } catch (NoSuchMethodException e) {
            System.err.printf("No method %s.%s() found!", instance.getClass().getName(), functionName);
            e.printStackTrace(System.err);
            return null;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    private static Object callFunction(Object instance, String functionName, Class klass1, Object arg1) {
        try {
            Method function = instance.getClass().getDeclaredMethod(functionName, klass1);
            return function.invoke(instance, arg1);
        } catch (NoSuchMethodException e) {
            System.err.printf("No method %s.%s() found!", instance.getClass().getName(), functionName);
            e.printStackTrace(System.err);
            return null;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    private static List<MeshAttachment> getDefaultAttachments(Object spineSceneUtil) {
        return (List<MeshAttachment>)callFunction(spineSceneUtil, "getDefaultAttachments");
    }

    private static Map<String, Animation> getAnimations(Object spineSceneUtil) {
        return (Map<String, Animation>)callFunction(spineSceneUtil, "getAnimations");
    }

    private static Animation getAnimation(Object spineSceneUtil, String arg1) {
        return (Animation)callFunction(spineSceneUtil, "getAnimation", String.class, arg1);
    }

    private static List<BaseSlot> getBaseSlots(Object spineSceneUtil) {
        return (List<BaseSlot>)callFunction(spineSceneUtil, "getBaseSlots");
    }

    private static List<Bone> getBones(Object spineSceneUtil) {
        return (List<Bone>)callFunction(spineSceneUtil, "getBones");
    }

    private static Bone getBone(Object spineSceneUtil, int index) {
        return (Bone)callFunction(spineSceneUtil, "getBone", int.class, index);
    }
    private static Bone getBone(Object spineSceneUtil, String name) {
        return (Bone)callFunction(spineSceneUtil, "getBone", String.class, name);
    }

    private static Object getField(Class<?> klass, Object instance, String fieldName) {
        try {
            Field field = klass.getField(fieldName);
            return field.get(instance);
        } catch (NoSuchFieldException e) {
            System.err.printf("No field %s.%s found!", klass.getName(), fieldName);
            return null;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    private static int getSlotSignalUnchanged(Object spineSceneUtil) {
        return (int) getField(spineSceneUtil.getClass(), null, "slotSignalUnchanged");
    }

    @Test
    public void testLoadingBones() throws Exception {
        Object scene = load("skeleton.json");
        assertEquals(9, getBones(scene).size());
        Bone root = getBone(scene, "root");
        Bone animated = getBone(scene, "bone_animated");
        Bone scale = getBone(scene, "bone_scale");
        Bone noScale = getBone(scene, "bone_noscale");
        Bone rotated = getBone(scene, "bone_rotated");
        assertEquals(getBone(scene, 0), root);
        assertEquals(getBone(scene, 1), animated);
        assertEquals(getBone(scene, 2), noScale);
        assertEquals(getBone(scene, 3), rotated);
        assertEquals(getBone(scene, 4), scale);
        assertEquals(2, noScale.index);
        assertTrue(scale.inheritScale);
        assertFalse(noScale.inheritScale);

        // Bone transforms
        assertTransform(root.localT, null, null, new Vector3d(2.0, 1.0, 1.0));
        assertTransform(root.worldT, null, null, new Vector3d(2.0, 1.0, 1.0));
        assertTransform(root.invWorldT, null, null, new Vector3d(0.5, 1.0, 1.0));

        assertTransform(scale.localT, new Point3d(100.0, 100.0, 0.0), null, new Vector3d(1.0, 1.0, 1.0));
        assertTransform(scale.worldT, new Point3d(200.0, 150.0, 0.0), null, new Vector3d(2.0, 1.0, 1.0));

        assertTransform(noScale.localT, new Point3d(100.0, 100.0, 0.0), null, new Vector3d(1.0, 1.0, 1.0));
        assertTransform(noScale.worldT, new Point3d(200.0, 150.0, 0.0), null, new Vector3d(1.0, 1.0, 1.0));

        double halfSqrt2 = 0.5 * Math.sqrt(2.0);
        assertTransform(rotated.localT, null, new Quat4d(0.0, 0.0, halfSqrt2, halfSqrt2), null);
    }

    private void assertMesh(MeshAttachment mesh, String path, float[] vertices, int[] triangles, int[] boneIndices, float[] boneWeights) {
        assertEquals(path, mesh.path);
        assertFloatArrays(vertices, mesh.vertices);
        assertIntArrays(triangles, mesh.triangles);
        assertIntArrays(boneIndices, mesh.boneIndices);
        assertFloatArrays(boneWeights, mesh.boneWeights);
    }

    @Test
    public void testLoadingMeshes() throws Exception {
        Object scene = load("skeleton.json");
        assertEquals(3, getDefaultAttachments(scene).size());
        assertMesh(getDefaultAttachments(scene).get(0), "test_sprite",
                new float[] {
                    100.0f, 0.0f, 0.0f, 0.0f, 1.0f,
                    100.0f, 100.0f, 0.0f, 0.0f, 0.0f,
                    300.0f, 0.0f, 0.0f, 0.5f, 1.0f,
                    300.0f, 100.0f, 0.0f, 0.5f, 0.0f,
                },
                new int[] {
                    0, 1, 2,
                    2, 1, 3,
                },
                new int[] {
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                },
                new float[] {
                    1.0f, 0.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 0.0f, 0.0f,
                }
        );
        assertMesh(getDefaultAttachments(scene).get(1), "test_sprite",
                new float[] {
                    100.0f, 0.0f, 0.0f, 0.5f, 1.0f,
                    -100.0f, 0.0f, 0.0f, 0.0f, 1.0f,
                    -100.0f, 100.0f, 0.0f, 0.0f, 0.0f,
                    100.0f, 100.0f, 0.0f, 0.5f, 0.0f
                },
                new int[] {
                    1, 2, 3,
                    1, 3, 0,
                },
                new int[] {
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                },
                new float[] {
                    1.0f, 0.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 0.0f, 0.0f,
                }
        );
        assertMesh(getDefaultAttachments(scene).get(2), "test_sprite",
                new float[] {
                    100.0f, 100.0f, 0.0f, 0.5f, 1.0f,
                    -100.0f, 100.0f, 0.0f, 0.0f, 1.0f,
                    -100.0f, 200.0f, 0.0f, 0.0f, 0.0f,
                    100.0f, 200.0f, 0.0f, 0.5f, 0.0f,
                },
                new int[] {
                        1, 2, 3,
                        1, 3, 0,
                },
                new int[] {
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 4, 0, 0, // third vertex mapped 50% to bone 0 and 4
                    0, 0, 0, 0,
                },
                new float[] {
                    1.0f, 0.0f, 0.0f, 0.0f,
                    1.0f, 0.0f, 0.0f, 0.0f,
                    0.5f, 0.5f, 0.0f, 0.0f, // third vertex mapped 50% to bone 0 and 4
                    1.0f, 0.0f, 0.0f, 0.0f,
                }
        );
    }

    private static void assertSimpleAnim(Object scene, String name, Property property, float[][] values) {
        Animation anim = getAnimation(scene, name);
        assertEquals(1.0, anim.duration, EPSILON);
        assertEquals(1, anim.tracks.size());
        AnimationTrack track = anim.tracks.get(0);
        assertEquals(getBone(scene, "bone_animated"), track.bone);
        assertEquals(property, track.property);
        assertEquals(2, track.keys.size());
        for (int i = 0; i < values.length; ++i) {
            assertFloatArrays(values[i], track.keys.get(i).value);
        }
    }

    private static void assertEvents(Object scene, String name, String eventId, Object[] values) {
        Animation anim = getAnimation(scene, name);
        assertEquals(1, anim.eventTracks.size());
        EventTrack track = anim.eventTracks.get(0);
        assertEquals(eventId, track.name);
        for (int i = 0; i < values.length; ++i) {
            Object o = values[i];
            if (o instanceof Integer) {
                assertEquals(o, track.keys.get(i).intPayload);
            } else if (o instanceof Float) {
                assertEquals(o, track.keys.get(i).floatPayload);
            } else if (o instanceof String) {
                assertEquals(o, track.keys.get(i).stringPayload);
            } else {
                assertTrue(false);
            }
        }
    }

    @Test
    public void testLoadingAnims() throws Exception {
        Object scene = load("skeleton.json");
        assertEquals(8, getAnimations(scene).size());

        assertSimpleAnim(scene, "anim_pos", Property.POSITION, new float[][] {new float[] {0.0f, 0.0f, 0.0f}, new float[] {100.0f, 0.0f, 0.0f}});
        assertSimpleAnim(scene, "anim_rot", Property.ROTATION, new float[][] {new float[] {0.0f}, new float[] {90.0f}});
        assertSimpleAnim(scene, "anim_scale", Property.SCALE, new float[][] {new float[] {1.0f, 1.0f, 1.0f}, new float[] {2.0f, 1.0f, 1.0f}});

        Animation animCurve = getAnimation(scene, "anim_curve");
        AnimationCurve curve = animCurve.tracks.get(0).keys.get(0).curve;
        assertFloatArrays(new float[] {
                0.0f, 0.0f, 1.0f, 1.0f
        }, new float[] {
                curve.x0, curve.y0, curve.x1, curve.y1
        });

        Animation animMulti = getAnimation(scene, "anim_multi");
        assertEquals(3, animMulti.tracks.size());
        assertEquals(1.0, animMulti.duration, EPSILON);

        Animation animStepped = getAnimation(scene, "anim_stepped");
        assertTrue(animStepped.tracks.get(0).keys.get(0).stepped);

        assertEvents(scene, "anim_event", "test_event", new Object[] {1, 0.5f, "test_string"});
    }

    @Test
    public void testSampleRotAnim() throws Exception {
        Object scene = load("simple_spine.json");
        Animation idle2 = getAnimation(scene, "idle2");
        AnimationTrack track = idle2.tracks.get(0);

        double sampleRate = 30.0;
        double spf = 1.0/sampleRate;
        double duration = idle2.duration;

        Rig.AnimationTrack.Builder animTrackBuilder = Rig.AnimationTrack.newBuilder();
        MockRotationBuilder rotBuilder = new MockRotationBuilder(animTrackBuilder);

        RigUtil.sampleTrack(track, rotBuilder, new Quat4d(0.0, 0.0, 0.0, 0.0), 0.0, duration, sampleRate, spf, true);
        double halfSqrt2 = Math.sqrt(2.0) / 2.0;
        int expectedNumRotSamples = ((int)Math.ceil(duration * sampleRate) + 2) * 4; // Quaternions
        Quat4d expectedInitRot = new Quat4d(0.0, 0.0, 0.0, 1.0);
        Quat4d expectedEndRot = new Quat4d(0.0, 0.0, halfSqrt2, halfSqrt2);

        assertEquals(expectedNumRotSamples, rotBuilder.GetRotationsCount());
        assertQuat4dEquals(expectedInitRot, rotBuilder.GetRotations(0), EPSILON);
        assertQuat4dEquals(expectedEndRot, rotBuilder.GetRotations(expectedNumRotSamples - 4), EPSILON);
    }

    @Test
    public void testSamplePosAnim() throws Exception {
        Object scene = load("curve_skeleton.json");
        Animation anim = getAnimation(scene, "animation");
        AnimationTrack track = anim.tracks.get(0);

        double sampleRate = 30.0;
        double spf = 1.0/sampleRate;
        double duration = anim.duration;

        Rig.AnimationTrack.Builder animTrackBuilder = Rig.AnimationTrack.newBuilder();
        MockPositionBuilder posBuilder = new MockPositionBuilder(animTrackBuilder);

        RigUtil.sampleTrack(track, posBuilder, new Point3d(0.0, 0.0, 0.0), 0.0, duration, sampleRate, spf, true);
        int expectedNumPosSamples = ((int)Math.ceil(duration * sampleRate) + 2) * 3; // Point3d
        Point3d expectedInitPos = new Point3d(0.0, 0.0, 0.0);
        Point3d expectedEndPos = new Point3d(100.0, 0.0, 0.0);

        assertEquals(expectedNumPosSamples, posBuilder.GetPositionsCount());
        assertPoint3dEquals(expectedInitPos, posBuilder.GetPositions(0), EPSILON);
        assertPoint3dEquals(expectedEndPos, posBuilder.GetPositions(expectedNumPosSamples - 3), EPSILON);
    }

    @Test
    public void testSamplePosSteppedAnim() throws Exception {
        Object scene = load("step_skeleton.json");
        Animation anim = getAnimation(scene, "animation");
        AnimationTrack track = anim.tracks.get(0);

        double sampleRate = 30.0;
        double spf = 1.0/sampleRate;
        double duration = anim.duration;
        boolean interpolate = true;
        boolean shouldSlerp = false;

        Rig.AnimationTrack.Builder animTrackBuilder = Rig.AnimationTrack.newBuilder();
        MockPositionBuilder posBuilder = new MockPositionBuilder(animTrackBuilder);

        RigUtil.sampleTrack(track, posBuilder, new Point3d(0.0, 0.0, 0.0), 0.0, duration, sampleRate, spf, interpolate);
        int expectedNumPosSamples = ((int)Math.ceil(duration * sampleRate) + 2) * 3; // Point3d
        Point3d expectedInitPos = new Point3d(0.0, 0.0, 0.0);
        Point3d expectedLowMidPos = new Point3d(0.0, 0.0, 0.0);
        Point3d expectedHighMidPos = new Point3d(0.0, 0.0, 0.0);
        Point3d expectedEndPos = new Point3d(100.0, 0.0, 0.0);

        int lowMidIndex = (int) expectedNumPosSamples / 2;
        lowMidIndex -= (lowMidIndex % 3);
        lowMidIndex -= 3; // "before" the mid point
        int highMidIndex = lowMidIndex + 6; // "after" the midpoint
        System.out.println("expectedNumPosSamples: " + expectedNumPosSamples + ", lowMidIndex: " + lowMidIndex);
        assertEquals(expectedNumPosSamples, posBuilder.GetPositionsCount());

        assertPoint3dEquals(expectedInitPos, posBuilder.GetPositions(0), EPSILON);
        assertPoint3dEquals(expectedLowMidPos, posBuilder.GetPositions(lowMidIndex), EPSILON);
        assertPoint3dEquals(expectedHighMidPos, posBuilder.GetPositions(highMidIndex), EPSILON);
        assertPoint3dEquals(expectedEndPos, posBuilder.GetPositions(expectedNumPosSamples - 3), EPSILON);
    }

    @Test
    public void testSampleVisibilityAnim() throws Exception {
        Object scene = load("visibility_skeleton.json");
        Animation anim = getAnimation(scene, "animation");
        SlotAnimationTrack track = anim.slotTracks.get(0);
        BaseSlot slot = getBaseSlots(scene).get(track.slot);

        double sampleRate = 30.0;
        double spf = 1.0/sampleRate;
        double duration = anim.duration;
        boolean interpolate = false;
        boolean shouldSlerp = false;

        // Original slot attachment names: _1, _2, _3, _4, _5
        // Slot attachment names are lost in the built data.
        // The default attachment for a slot is added first, in our case "_5" which gets the index 0.
        int[] attachmentIndices = {1, 2, 3, 4, 0, 0};
        int expectedSampleCountPerMesh = attachmentIndices.length;

        MeshAnimationTrack.Builder trackBuilder = MeshAnimationTrack.newBuilder();
        trackBuilder.setMeshSlot(0);
        MockAttachmentBuilder attachmentBuilder = new MockAttachmentBuilder(trackBuilder);
        RigUtil.sampleTrack(track, attachmentBuilder, slot.activeAttachment, 0.0, duration, sampleRate, spf, interpolate);
        assertEquals(expectedSampleCountPerMesh, attachmentBuilder.GetMeshAttachmentCount());

        for (int j=0; j < expectedSampleCountPerMesh; ++j) {
            assertEquals(attachmentIndices[j], attachmentBuilder.GetMeshAttachment(j));
        }
    }

    @Test
    public void testSampleDrawOrderAnim() throws Exception {
        Object scene = load("draw_order_skeleton.json");
        Animation anim = getAnimation(scene, "animation");

        assertEquals(3, anim.slotTracks.size()); // should only contain 3 draw_order slot tracks

        SlotAnimationTrack track_4 = anim.slotTracks.get(0);
        SlotAnimationTrack track_3 = anim.slotTracks.get(1);
        SlotAnimationTrack track_5 = anim.slotTracks.get(2);

        double sampleRate = 30.0;
        double spf = 1.0/sampleRate;
        double duration = anim.duration;
        boolean interpolate = false;
        boolean shouldSlerp = false;


        int expectedSampleCountPerMesh = 6;
        MeshAnimationTrack.Builder trackBuilder = MeshAnimationTrack.newBuilder();

        // Mesh "_3" [0, 2, 1, 0, 0, 0]
        MockDrawOrderBuilder drawOrderBuilder = new MockDrawOrderBuilder(trackBuilder);
        RigUtil.sampleTrack(track_3, drawOrderBuilder, getSlotSignalUnchanged(scene), 0.0, duration, sampleRate, spf, interpolate);
        assertEquals(expectedSampleCountPerMesh, drawOrderBuilder.GetOrderOffsetCount());
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(0));
        assertEquals(2, drawOrderBuilder.GetOrderOffset(1));
        assertEquals(1, drawOrderBuilder.GetOrderOffset(2));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(3));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(4));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(5));
        trackBuilder.clear();

        // Mesh "_4" [0, 2, 1, 0, 0, 0]
        drawOrderBuilder = new MockDrawOrderBuilder(trackBuilder);
        RigUtil.sampleTrack(track_4, drawOrderBuilder, getSlotSignalUnchanged(scene), 0.0, duration, sampleRate, spf, interpolate);
        assertEquals(expectedSampleCountPerMesh, drawOrderBuilder.GetOrderOffsetCount());
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(0));
        assertEquals(2, drawOrderBuilder.GetOrderOffset(1));
        assertEquals(1, drawOrderBuilder.GetOrderOffset(2));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(3));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(4));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(5));
        trackBuilder.clear();

        // Mesh "_5" [0, 0, 4, 4, 0, 0]
        drawOrderBuilder = new MockDrawOrderBuilder(trackBuilder);
        RigUtil.sampleTrack(track_5, drawOrderBuilder, getSlotSignalUnchanged(scene), 0.0, duration, sampleRate, spf, interpolate);
        assertEquals(expectedSampleCountPerMesh, drawOrderBuilder.GetOrderOffsetCount());
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(0));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(1));
        assertEquals(4, drawOrderBuilder.GetOrderOffset(2));
        assertEquals(4, drawOrderBuilder.GetOrderOffset(3));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(4));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(5));
    }

    @Test
    public void testSampleDrawOrderResetKey() throws Exception {
        Object scene = load("draw_order_skeleton_sparse_duplicates.json");
        Animation anim = getAnimation(scene, "animation");

        assertEquals(3, anim.slotTracks.size());

        SlotAnimationTrack track_4 = anim.slotTracks.get(0);

        double sampleRate = 30.0;
        double spf = 1.0/sampleRate;
        double duration = anim.duration;
        boolean interpolate = false;
        boolean shouldSlerp = false;
        int expectedSampleCountPerMesh = 15;

        MeshAnimationTrack.Builder trackBuilder = MeshAnimationTrack.newBuilder();
        MockDrawOrderBuilder drawOrderBuilder = new MockDrawOrderBuilder(trackBuilder);

        // Mesh "_4" [0, 0, 0, 2, 2, 2, 1, 1, 1, 0xDEAD, 0xDEAD, 0xDEAD, 0, 0, 0]
        drawOrderBuilder = new MockDrawOrderBuilder(trackBuilder);
        RigUtil.sampleTrack(track_4, drawOrderBuilder, getSlotSignalUnchanged(scene), 0.0, duration, sampleRate, spf, interpolate);
        assertEquals(expectedSampleCountPerMesh, drawOrderBuilder.GetOrderOffsetCount());
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(0));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(1));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(2));
        assertEquals(2, drawOrderBuilder.GetOrderOffset(3));
        assertEquals(2, drawOrderBuilder.GetOrderOffset(4));
        assertEquals(2, drawOrderBuilder.GetOrderOffset(5));
        assertEquals(1, drawOrderBuilder.GetOrderOffset(6));
        assertEquals(1, drawOrderBuilder.GetOrderOffset(7));
        assertEquals(1, drawOrderBuilder.GetOrderOffset(8));
        assertEquals(0, drawOrderBuilder.GetOrderOffset(9));
        assertEquals(0, drawOrderBuilder.GetOrderOffset(10));
        assertEquals(0, drawOrderBuilder.GetOrderOffset(11));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(12));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(13));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(14));
        trackBuilder.clear();
    }

    @Test
    public void testSampleSparseDrawOrderAnim() throws Exception {
        Object scene = load("draw_order_skeleton_sparse.json");
        Animation anim = getAnimation(scene, "animation");

        assertEquals(3, anim.slotTracks.size());

        SlotAnimationTrack track_4 = anim.slotTracks.get(0);
        SlotAnimationTrack track_3 = anim.slotTracks.get(1);
        SlotAnimationTrack track_5 = anim.slotTracks.get(2);

        double sampleRate = 30.0;
        double spf = 1.0/sampleRate;
        double duration = anim.duration;
        boolean interpolate = false;
        boolean shouldSlerp = false;

        int expectedSampleCountPerMesh = 15;

        MeshAnimationTrack.Builder trackBuilder = MeshAnimationTrack.newBuilder();

        // Mesh "_3" [0, 0, 0, 2, 2, 2, 1, 1, 1, 0, 0, 0, 0, 0]
        MockDrawOrderBuilder drawOrderBuilder = new MockDrawOrderBuilder(trackBuilder);
        RigUtil.sampleTrack(track_3, drawOrderBuilder, getSlotSignalUnchanged(scene), 0.0, duration, sampleRate, spf, interpolate);
        assertEquals(expectedSampleCountPerMesh, drawOrderBuilder.GetOrderOffsetCount());
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(0));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(1));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(2));
        assertEquals(2, drawOrderBuilder.GetOrderOffset(3));
        assertEquals(2, drawOrderBuilder.GetOrderOffset(4));
        assertEquals(2, drawOrderBuilder.GetOrderOffset(5));
        assertEquals(1, drawOrderBuilder.GetOrderOffset(6));
        assertEquals(1, drawOrderBuilder.GetOrderOffset(7));
        assertEquals(1, drawOrderBuilder.GetOrderOffset(8));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(9));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(10));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(11));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(12));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(13));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(14));
        trackBuilder.clear();

        // Mesh "_4" [0, 0, 0, 2, 2, 2, 1, 1, 1, 0, 0, 0, 0, 0]
        drawOrderBuilder = new MockDrawOrderBuilder(trackBuilder);
        RigUtil.sampleTrack(track_4, drawOrderBuilder, getSlotSignalUnchanged(scene), 0.0, duration, sampleRate, spf, interpolate);
        assertEquals(expectedSampleCountPerMesh, drawOrderBuilder.GetOrderOffsetCount());
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(0));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(1));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(2));
        assertEquals(2, drawOrderBuilder.GetOrderOffset(3));
        assertEquals(2, drawOrderBuilder.GetOrderOffset(4));
        assertEquals(2, drawOrderBuilder.GetOrderOffset(5));
        assertEquals(1, drawOrderBuilder.GetOrderOffset(6));
        assertEquals(1, drawOrderBuilder.GetOrderOffset(7));
        assertEquals(1, drawOrderBuilder.GetOrderOffset(8));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(9));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(10));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(11));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(12));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(13));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(14));
        trackBuilder.clear();

        // Mesh "_5" [0, 0, 0, 0, 0, 0, 4, 4, 4, 4, 4, 4, 0, 0]
        drawOrderBuilder = new MockDrawOrderBuilder(trackBuilder);
        RigUtil.sampleTrack(track_5, drawOrderBuilder, getSlotSignalUnchanged(scene), 0.0, duration, sampleRate, spf, interpolate);
        assertEquals(expectedSampleCountPerMesh, drawOrderBuilder.GetOrderOffsetCount());
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(0));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(1));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(2));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(3));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(4));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(5));
        assertEquals(4, drawOrderBuilder.GetOrderOffset(6));
        assertEquals(4, drawOrderBuilder.GetOrderOffset(7));
        assertEquals(4, drawOrderBuilder.GetOrderOffset(8));
        assertEquals(4, drawOrderBuilder.GetOrderOffset(9));
        assertEquals(4, drawOrderBuilder.GetOrderOffset(10));
        assertEquals(4, drawOrderBuilder.GetOrderOffset(11));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(12));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(13));
        assertEquals(getSlotSignalUnchanged(scene), drawOrderBuilder.GetOrderOffset(14));
    }

    @Test
    public void testEmptyScene() throws Exception {
        InputStream input = null;
        try {
            input = getClass().getResourceAsStream("empty.json");
            Object scene = load(input);
            assertEquals(1, getBones(scene).size());
            assertEquals(0, getDefaultAttachments(scene).size());
            assertEquals(0, getAnimations(scene).size());
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    @Test
    public void testSampleScenes() throws Exception {
        for (int i = 1; i < 9; ++i) {
            InputStream input = null;
            try {
                input = getClass().getResourceAsStream(String.format("sample%d.json", i));
                Object scene = load(input);
                assertTrue(0 < getBones(scene).size());
                assertTrue(0 < getDefaultAttachments(scene).size());
                assertTrue(0 < getAnimations(scene).size());
            } finally {
                IOUtils.closeQuietly(input);
            }
        }
    }
}
