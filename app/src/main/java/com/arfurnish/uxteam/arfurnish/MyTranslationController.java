package com.arfurnish.uxteam.arfurnish;

import android.os.Debug;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.MathHelper;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.utilities.Preconditions;
import com.google.ar.sceneform.ux.BaseTransformableNode;
import com.google.ar.sceneform.ux.DragGesture;
import com.google.ar.sceneform.ux.DragGestureRecognizer;
import com.google.ar.sceneform.ux.TranslationController;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

public class MyTranslationController extends TranslationController {
    private static final String TAG = "MyActivity";
    @Nullable
    private HitResult lastArHitResult;
    @Nullable
    private Vector3 desiredLocalPosition;
    @Nullable
    private Quaternion desiredLocalRotation;
    private final Vector3 initialForwardInLocal = new Vector3();
    private EnumSet<Plane.Type> allowedPlaneTypes = EnumSet.allOf(Plane.Type.class);
    private static final float LERP_SPEED = 12.0F;
    private static final float POSITION_LENGTH_THRESHOLD = 0.01F;
    private static final float ROTATION_DOT_THRESHOLD = 0.99F;

    public MyTranslationController(BaseTransformableNode transformableNode, DragGestureRecognizer gestureRecognizer) {
        super(transformableNode, gestureRecognizer);
    }

    public void setAllowedPlaneTypes(EnumSet<Plane.Type> allowedPlaneTypes) {
        this.allowedPlaneTypes = allowedPlaneTypes;
    }

    public EnumSet<Plane.Type> getAllowedPlaneTypes() {
        return this.allowedPlaneTypes;
    }

    public void onUpdated(Node node, FrameTime frameTime) {
        this.updatePosition(frameTime);
        this.updateRotation(frameTime);
    }

    public boolean isTransforming() {
        return super.isTransforming() || this.desiredLocalRotation != null || this.desiredLocalPosition != null;
    }

    long currentTime;
    boolean isFirstTouch = false;
    boolean pasueTranfoation = false;

    public boolean canStartTransformation(DragGesture gesture) {
        currentTime = System.currentTimeMillis();
        Log.d(TAG, "Lalala: " + currentTime);

        Node targetNode = gesture.getTargetNode();
        if (targetNode == null) {
            return false;
        } else {
            BaseTransformableNode transformableNode = this.getTransformableNode();
            if (targetNode != transformableNode && !targetNode.isDescendantOf(transformableNode)) {
                return false;
            } else if (!transformableNode.isSelected() && !transformableNode.select()) {
                return false;
            } else {
                Vector3 initialForwardInWorld = transformableNode.getForward();
                Node parent = transformableNode.getParent();
                if (parent != null) {
                    this.initialForwardInLocal.set(parent.worldToLocalDirection(initialForwardInWorld));
                } else {
                    this.initialForwardInLocal.set(initialForwardInWorld);
                }
                isFirstTouch = true;
                return true;
            }
        }
    }


    public void onContinueTransformation(DragGesture gesture) {

        Scene scene = this.getTransformableNode().getScene();
        if (scene != null) {
            Frame frame = ((ArSceneView) scene.getView()).getArFrame();
            if (frame != null) {
                Camera arCamera = frame.getCamera();
                if (arCamera.getTrackingState() == TrackingState.TRACKING) {
                    Vector3 position = gesture.getPosition();
                    List<HitResult> hitResultList = frame.hitTest(position.x, position.y);

                    for (int i = 0; i < hitResultList.size(); ++i) {
                        HitResult hit = (HitResult) hitResultList.get(i);
                        Trackable trackable = hit.getTrackable();
                        Pose pose = hit.getHitPose();
                        if (trackable instanceof Plane) {
                            Plane plane = (Plane) trackable;
                            if (plane.isPoseInPolygon(pose) && this.allowedPlaneTypes.contains(plane.getType())) {
                                this.desiredLocalPosition = new Vector3(pose.tx(), pose.ty(), pose.tz());
                                this.desiredLocalRotation = new Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw());
                                Node parent = this.getTransformableNode().getParent();
                                if (parent != null && this.desiredLocalPosition != null && this.desiredLocalRotation != null) {

                                    this.desiredLocalPosition = parent.worldToLocalPoint(this.desiredLocalPosition);
                                    this.desiredLocalRotation = Quaternion.multiply(parent.getWorldRotation().inverted(), (Quaternion) Preconditions.checkNotNull(this.desiredLocalRotation));
                                }

                                this.desiredLocalRotation = this.calculateFinalDesiredLocalRotation((Quaternion) Preconditions.checkNotNull(this.desiredLocalRotation));
                                this.lastArHitResult = hit;
                                break;
                            }
                        }
                    }

                }
            }
        }
    }

    public void onEndTransformation(DragGesture gesture) {
        HitResult hitResult = this.lastArHitResult;
        if (hitResult != null) {
            if (hitResult.getTrackable().getTrackingState() == TrackingState.TRACKING) {
                AnchorNode anchorNode = this.getAnchorNodeOrDie();
                Anchor oldAnchor = anchorNode.getAnchor();
                if (oldAnchor != null) {
                    oldAnchor.detach();
                }

                Anchor newAnchor = hitResult.createAnchor();
                Vector3 worldPosition = this.getTransformableNode().getWorldPosition();
                Quaternion worldRotation = this.getTransformableNode().getWorldRotation();
                Quaternion finalDesiredWorldRotation = worldRotation;
                Quaternion desiredLocalRotation = this.desiredLocalRotation;
                if (desiredLocalRotation != null) {
                    this.getTransformableNode().setLocalRotation(desiredLocalRotation);
                    finalDesiredWorldRotation = this.getTransformableNode().getWorldRotation();
                }

                anchorNode.setAnchor(newAnchor);
                this.getTransformableNode().setWorldRotation(finalDesiredWorldRotation);
                Vector3 initialForwardInWorld = this.getTransformableNode().getForward();
                this.initialForwardInLocal.set(anchorNode.worldToLocalDirection(initialForwardInWorld));
                this.getTransformableNode().setWorldRotation(worldRotation);
                if (!pasueTranfoation)
                    this.getTransformableNode().setWorldPosition(worldPosition);
            }

            this.desiredLocalPosition = Vector3.zero();
            this.desiredLocalRotation = this.calculateFinalDesiredLocalRotation(Quaternion.identity());
            isFirstTouch = false;
            pasueTranfoation = false;
        }
    }

    private AnchorNode getAnchorNodeOrDie() {
        Node parent = this.getTransformableNode().getParent();
        if (!(parent instanceof AnchorNode)) {
            throw new IllegalStateException("TransformableNode must have an AnchorNode as a parent.");
        } else {
            return (AnchorNode) parent;
        }
    }

    private void updatePosition(FrameTime frameTime) {
        if (pasueTranfoation)
            return;
        Vector3 desiredLocalPosition = this.desiredLocalPosition;
        if (desiredLocalPosition != null) {
            Vector3 localPosition = this.getTransformableNode().getLocalPosition();

//            float val = Vector3.subtract(localPosition, desiredLocalPosition).length();
//            Log.d(TAG, "my_text " + (System.currentTimeMillis() - currentTime));
//            if (isFirstTouch) {
//                if (System.currentTimeMillis() - currentTime > 1) {
//                    pasueTranfoation = true;
//
//                } else if (val > 0.25f) isFirstTouch = false;
//                return;
//            }


            float lerpFactor = MathHelper.clamp(frameTime.getDeltaSeconds() * 12.0F, 0.0F, 1.0F);
            localPosition = Vector3.lerp(localPosition, desiredLocalPosition, lerpFactor);
            float lengthDiff = Math.abs(Vector3.subtract(desiredLocalPosition, localPosition).length());
            if (lengthDiff <= 0.01F) {
                localPosition = desiredLocalPosition;
                this.desiredLocalPosition = null;
            }

            this.getTransformableNode().setLocalPosition(localPosition);
        }
    }

    private void updateRotation(FrameTime frameTime) {
        Quaternion desiredLocalRotation = this.desiredLocalRotation;
        if (desiredLocalRotation != null) {
            Quaternion localRotation = this.getTransformableNode().getLocalRotation();
            float lerpFactor = MathHelper.clamp(frameTime.getDeltaSeconds() * 12.0F, 0.0F, 1.0F);
            localRotation = Quaternion.slerp(localRotation, desiredLocalRotation, lerpFactor);
            float dot = Math.abs(dotQuaternion(localRotation, desiredLocalRotation));
            if (dot >= 0.99F) {
                localRotation = desiredLocalRotation;
                this.desiredLocalRotation = null;
            }

            this.getTransformableNode().setLocalRotation(localRotation);
        }
    }

    private Quaternion calculateFinalDesiredLocalRotation(Quaternion desiredLocalRotation) {
        Vector3 rotatedUp = Quaternion.rotateVector(desiredLocalRotation, Vector3.up());
        desiredLocalRotation = Quaternion.rotationBetweenVectors(Vector3.up(), rotatedUp);
        Quaternion forwardInLocal = Quaternion.rotationBetweenVectors(Vector3.forward(), this.initialForwardInLocal);
        desiredLocalRotation = Quaternion.multiply(desiredLocalRotation, forwardInLocal);
        return desiredLocalRotation.normalized();
    }

    private static float dotQuaternion(Quaternion lhs, Quaternion rhs) {
        return lhs.x * rhs.x + lhs.y * rhs.y + lhs.z * rhs.z + lhs.w * rhs.w;
    }
}
