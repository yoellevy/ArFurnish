package com.arfurnish.uxteam.arfurnish;

import com.google.ar.sceneform.ux.BaseTransformableNode;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;
import com.google.ar.sceneform.ux.TranslationController;
import com.google.ar.sceneform.ux.TwistGestureRecognizer;

public class MyNode extends BaseTransformableNode {
    private final TranslationController translationController;

    public MyNode(TransformationSystem transformationSystem) {
        super(transformationSystem);
        this.translationController = new MyTranslationController(this, transformationSystem.getDragRecognizer());
        this.addTransformationController(this.translationController);
    }

    public TranslationController getTranslationController() {
        return this.translationController;
    }

}
