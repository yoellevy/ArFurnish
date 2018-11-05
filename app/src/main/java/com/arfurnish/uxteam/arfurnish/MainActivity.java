package com.arfurnish.uxteam.arfurnish;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity {

    private ArFragment fragment;

    // no need TODO: delete
    private PointerDrawable pointer = new PointerDrawable();
    private boolean isTracking;
    private boolean isHitting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        // TODO: remove
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        fragment = (ArFragment)
                getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);

        //called every frame
        fragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            fragment.onUpdate(frameTime);
            //our function
            onUpdate();
        });

        //Insertig thumbnails to drawer
        initializeGallery();
    }

    //return if tracking in updated
    private boolean updateTracking() {
        Frame frame = fragment.getArSceneView().getArFrame();
        boolean wasTracking = isTracking;
        isTracking = frame != null &&
                frame.getCamera().getTrackingState() == TrackingState.TRACKING;
        return isTracking != wasTracking;
    }

    private boolean updateHitTest() {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        boolean wasHitting = isHitting;
        isHitting = false;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    isHitting = true;
                    break;
                }
            }
        }
        return wasHitting != isHitting;
    }

    private android.graphics.Point getScreenCenter() {
        View vw = findViewById(android.R.id.content);
        return new android.graphics.Point(vw.getWidth() / 2, vw.getHeight() / 2);
    }

    private void onUpdate() {
        boolean trackingChanged = updateTracking();
        View contentView = findViewById(android.R.id.content);
        if (trackingChanged) {
            if (isTracking) {
                contentView.getOverlay().add(pointer);
            } else {
                contentView.getOverlay().remove(pointer);
            }
            contentView.invalidate();
        }

        if (isTracking) {
            boolean hitTestChanged = updateHitTest();
            if (hitTestChanged) {
                pointer.setEnabled(isHitting);
                contentView.invalidate();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private static class MyDragShadowBuilder extends View.DragShadowBuilder {

        // The drag shadow image, defined as a drawable thing
        private static Drawable shadow;

        // Defines the constructor for myDragShadowBuilder
        public MyDragShadowBuilder(View v) {

            // Stores the View parameter passed to myDragShadowBuilder.
            super(v);

            // Creates a draggable image that will fill the Canvas provided by the system.
            shadow = new ColorDrawable(Color.LTGRAY);
        }

        // Defines a callback that sends the drag shadow dimensions and touch point back to the
        // system.
        @Override
        public void onProvideShadowMetrics(Point size, Point touch) {
            // Defines local variables
            int width, height;

            // Sets the width of the shadow to half the width of the original View
            width = getView().getWidth() / 2;

            // Sets the height of the shadow to half the height of the original View
            height = getView().getHeight() / 2;

            // The drag shadow is a ColorDrawable. This sets its dimensions to be the same as the
            // Canvas that the system will provide. As a result, the drag shadow will fill the
            // Canvas.
            shadow.setBounds(0, 0, width, height);

            // Sets the size parameter's width and height values. These get back to the system
            // through the size parameter.
            size.set(width, height);

            // Sets the touch point's position to be in the middle of the drag shadow
            touch.set(width / 2, height / 2);
        }

        // Defines a callback that draws the drag shadow in a Canvas that the system constructs
        // from the dimensions passed in onProvideShadowMetrics().
        @Override
        public void onDrawShadow(Canvas canvas) {

            // Draws the ColorDrawable in the Canvas passed in from the system.
            shadow.draw(canvas);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected class myDragEventListener implements View.OnDragListener {

        String model_name;

        myDragEventListener(String name) {
            model_name = name;
        }

        // This is the method that the system calls when it dispatches a drag event to the
        // listener.
        public boolean onDrag(View v, DragEvent event) {

//            Log.i("maya", "inside onDrag");

            // Defines a variable to store the action type for the incoming event
            final int action = event.getAction();

            // Handles each of the expected events
            switch (action) {

                case DragEvent.ACTION_DRAG_STARTED:
                    Log.i("maya", "ACTION_DRAG_STARTED, v tag: " + v.getTag());
                    // Determines if this View can accept the dragged data
                    return (event.getClipDescription().hasMimeType
                            (ClipDescription.MIMETYPE_TEXT_PLAIN));

                case DragEvent.ACTION_DRAG_ENTERED:
                    Log.i("maya", "ACTION_DRAG_ENTERED, v tag: " + v.getTag());
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:
                    Log.i("maya", "ACTION_DRAG_LOCATION, v tag: " + v.getTag());
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    Log.i("maya", "ACTION_DRAG_EXITED, v tag: " + v.getTag());
                    return true;

                case DragEvent.ACTION_DROP:
                    Log.i("maya", "ACTION_DROP, v tag: " + v.getTag());

                    // Gets the item containing the dragged data
                    ClipData.Item item = event.getClipData().getItemAt(0);

                    // Gets the text data from the item.
                    String dragData = (String) item.getText();

                    // Displays a message containing the dragged data.
                    Toast.makeText(MainActivity.this, "Dragged data is " + dragData, Toast.LENGTH_LONG).show();

                    // Returns true. DragEvent.getResult() will return true.
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    Log.i("maya", "ACTION_DRAG_ENDED, v tag: " + v.getTag());
                    // Does a getResult(), and displays what happened.
                    Log.i("maya", "v tag: " + v.getTag() + " model: " + model_name);
//                    Toast.makeText(MainActivity.this, "v tag: " + v.getTag() + " model: " + model_name, Toast.LENGTH_LONG).show();
                    Toast.makeText(MainActivity.this, selected_tag_name, Toast.LENGTH_LONG).show();
                    if (selected_tag_name == v.getTag().toString()) {
                        Toast.makeText(MainActivity.this, "The drop was handled.", Toast.LENGTH_LONG).show();
                        addObject(Uri.parse(model_name));
                    }
                    if (event.getResult()) {
//                        Toast.makeText(MainActivity.this, "The drop was handled.", Toast.LENGTH_LONG).show();
//                        addObject(Uri.parse(model_name));

                    } else {
//                        Toast.makeText(MainActivity.this, "The drop didn't work.", Toast.LENGTH_LONG).show();
//                        addObject(Uri.parse(model_name));
                    }

                    // returns true; the value is ignored.
                    return true;

                // An unknown action type was received.
                default:
                    Log.e("DragDrop Example", "Unknown action type received by OnDragListener.");
                    break;
            }

            return false;
        }
    }

    String selected_tag_name ="";

    public class MyLongClickListener implements View.OnTouchListener {



        //        ImageView imgView;
//        MyLongClickListener(ImageView imgView) {
//            this.imgView=imgView;
//            Log.i("maya", "imgView id in constructor: " + imgView.getId());
//        }
        // Defines the one method for the interface, which is called when the View is long-clicked
        @Override
        public boolean onTouch(View v, MotionEvent motionEvent){
            selected_tag_name = v.getTag().toString();
            Log.i("maya", "onLongClick, v tag: " + v.getTag());
//            Log.i("maya", "imgView tag: " + imgView.getTag());

            // Create a new ClipData.
            // This is done in two steps to provide clarity. The convenience method
            // ClipData.newPlainText() can create a plain text ClipData in one step.

            // Create a new ClipData.Item from the ImageView object's tag
            ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
//            Log.i("maya", "1");

            // Create a new ClipData using the tag as a label, the plain text MIME type, and
            // the already-created item. This will create a new ClipDescription object within the
            // ClipData, and set its MIME type entry to "text/plain"
            ClipData dragData = new ClipData((CharSequence) v.getTag(),
                    new String[]{ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
//            Log.i("maya", "2");
            // Instantiates the drag shadow builder.
            View.DragShadowBuilder myShadow = new MyDragShadowBuilder(v);

//            Log.i("maya", "3");
            // Starts the drag
            v.startDragAndDrop(dragData,  // the data to be dragged
                    myShadow,  // the drag shadow builder
                    null,      // no need to use local data
                    0);          // flags (not currently used, set to 0)
//            Log.i("maya", "4");
            return true;
        }
    }

    private void initializeGallery() {
        Log.i("maya", "inside initializeGallery");
        LinearLayout gallery = findViewById(R.id.gallery_layout);

        ImageView sofa = new ImageView(this);
        sofa.setImageResource(R.drawable.droid_thumb);
        sofa.setContentDescription("sofa");
//        andy.setImageBitmap(mIconBitmap);
        sofa.setTag("sofa");
//        andy.setOnClickListener(view ->{addObject(Uri.parse("andy.sfb"));});

        // Sets the drag event listener for the View
        sofa.setOnDragListener(new myDragEventListener("sofa.sfb"));
//        andy.setOnLongClickListener(new MyLongClickListener());
        sofa.setOnTouchListener(new MyLongClickListener());
        gallery.addView(sofa);

        ImageView bookS = new ImageView(this);
        bookS.setImageResource(R.drawable.cabin_thumb);
        bookS.setContentDescription("book shelves");
        bookS.setTag("book shelves");

        bookS.setOnDragListener(new myDragEventListener("bookS.sfb"));
        bookS.setOnTouchListener(new MyLongClickListener());

        gallery.addView(bookS);

        ImageView lamp = new ImageView(this);
        lamp.setImageResource(R.drawable.house_thumb);
        lamp.setContentDescription("lamp");
        lamp.setTag("lamp");

        lamp.setOnDragListener(new myDragEventListener("lamp.sfb"));
        lamp.setOnTouchListener(new MyLongClickListener());
        gallery.addView(lamp);

        ImageView chair = new ImageView(this);
        chair.setImageResource(R.drawable.igloo_thumb);
        chair.setContentDescription("chair");
        chair.setTag("chair");
        chair.setOnDragListener(new myDragEventListener("chair.sfb"));
        chair.setOnTouchListener(new MyLongClickListener());
        gallery.addView(chair);
    }

    private void addObject(Uri model) {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    placeObject(fragment, hit.createAnchor(), model);
                    break;

                }
            }
        }
    }

    private void placeObject(ArFragment fragment, Anchor anchor, Uri model) {
        CompletableFuture<Void> renderableFuture =
                ModelRenderable.builder()
                        .setSource(fragment.getContext(), model)
                        .build()
                        .thenAccept(renderable -> addNodeToScene(fragment, anchor, renderable))
                        .exceptionally((throwable -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage(throwable.getMessage())
                                    .setTitle("Codelab error!");
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            return null;
                        }));
    }

    private void addNodeToScene(ArFragment fragment, Anchor anchor, Renderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        MyNode node = new MyNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }
}
