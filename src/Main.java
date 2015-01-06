package nz.gen.geek_central.view_cache_demo;
/*
    Demonstration of how to do smooth scrolling of a complex
    image by careful caching of bitmaps--app mainline.

    Copyright 2011, 2012 by Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.
*/

import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;

public class Main extends android.app.Activity
  {
    protected ViewGroup MainLayout;
    protected android.widget.ZoomControls Zoomer;
    protected SampleDrawer DrawWhat;
    protected DrawView TheDrawView;

    protected class ControlDialog
        extends android.app.Dialog
        implements android.content.DialogInterface.OnDismissListener
      {
        final android.content.Context ctx;
        RadioGroup CachingButtons, ComplexityButtons;

        public ControlDialog
          (
            android.content.Context ctx
          )
          {
            super(ctx);
            this.ctx = ctx;
          } /*ControlDialog*/

        @Override
        public void onCreate
          (
            android.os.Bundle savedInstanceState
          )
          {
            setTitle(R.string.settings);
            final LinearLayout MainLayout = new LinearLayout(ctx);
            MainLayout.setOrientation(LinearLayout.HORIZONTAL);
            setContentView(MainLayout);
            final ViewGroup.LayoutParams ButtonLayoutParams =
                new ViewGroup.LayoutParams
                  (
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                  );
            final ViewGroup.LayoutParams GroupLayoutParams =
                new ViewGroup.LayoutParams
                  (
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.FILL_PARENT
                  );
              {
                final LinearLayout ButtonGroupLayout = new LinearLayout(ctx);
                ButtonGroupLayout.setOrientation(LinearLayout.VERTICAL);
                final TextView Heading = new TextView(ctx);
                Heading.setText(R.string.cache_control);
                ButtonGroupLayout.addView(Heading);
                CachingButtons = new RadioGroup(ctx);
                final RadioButton CachingOn = new RadioButton(ctx);
                CachingOn.setText(R.string.on);
                CachingOn.setId(1);
                final RadioButton CachingOff = new RadioButton(ctx);
                CachingOff.setText(R.string.off);
                CachingOff.setId(0);
                CachingButtons.addView(CachingOn, 0, ButtonLayoutParams);
                CachingButtons.addView(CachingOff, 1, ButtonLayoutParams);
                ButtonGroupLayout.addView(CachingButtons);
                MainLayout.addView(ButtonGroupLayout, GroupLayoutParams);
              }
              {
                final LinearLayout ButtonGroupLayout = new LinearLayout(ctx);
                ButtonGroupLayout.setOrientation(LinearLayout.VERTICAL);
                final TextView Heading = new TextView(ctx);
                Heading.setText(R.string.complexity);
                ButtonGroupLayout.addView(Heading);
                ComplexityButtons = new RadioGroup(ctx);
                final RadioButton ComplexityHigh = new RadioButton(ctx);
                ComplexityHigh.setText(R.string.high);
                ComplexityHigh.setId(1);
                final RadioButton ComplexityLow = new RadioButton(ctx);
                ComplexityLow.setText(R.string.low);
                ComplexityLow.setId(0);
                ComplexityButtons.addView(ComplexityHigh, 0, ButtonLayoutParams);
                ComplexityButtons.addView(ComplexityLow, 1, ButtonLayoutParams);
                ButtonGroupLayout.addView(ComplexityButtons);
                MainLayout.addView(ButtonGroupLayout, GroupLayoutParams);
              }
            CachingButtons.check(TheDrawView.GetUseCaching() ? 1 : 0);
            ComplexityButtons.check(DrawWhat.GetComplexity() ? 1 : 0);
            setOnDismissListener(this);
          } /*onCreate*/

        @Override
        public void onDismiss
          (
            android.content.DialogInterface TheDialog
          )
          {
            TheDrawView.SetUseCaching(CachingButtons.getCheckedRadioButtonId() != 0);
            final boolean NewComplexity = ComplexityButtons.getCheckedRadioButtonId() != 0;
            if (NewComplexity != DrawWhat.GetComplexity())
              {
                DrawWhat.SetComplexity(NewComplexity);
                TheDrawView.ForgetViewCache();
                TheDrawView.invalidate();
              } /*if*/
          } /*onClick*/

      } /*ControlDialog*/

    @Override
    public void onCreate
      (
        android.os.Bundle SavedInstanceState
      )
      {
          { /* show some info about the display */
            final android.view.Display MainDisplay = getWindowManager().getDefaultDisplay();
            final android.util.DisplayMetrics MainMetrics = new android.util.DisplayMetrics();
            MainDisplay.getMetrics(MainMetrics);
            System.err.printf
              (
                    "view_cache_demo: display %d size %dx%d, orientation %d,"
                +
                    " pixel format 0x%x, refresh %.2fHz\n",
                MainDisplay.getDisplayId(),
                MainDisplay.getWidth(),
                MainDisplay.getHeight(),
                MainDisplay.getOrientation(),
                MainDisplay.getPixelFormat(),
                MainDisplay.getRefreshRate()
              );
            System.err.printf
              (
                    " metrics: density %.2f, densityDpi %d, scaledDensity %.2f,"
                +
                    " size %dx%d, dpi %.2fx%.2f\n",
                MainMetrics.density,
                MainMetrics.densityDpi,
                MainMetrics.scaledDensity,
                MainMetrics.widthPixels,
                MainMetrics.heightPixels,
                MainMetrics.xdpi,
                MainMetrics.ydpi
              );
          }
          { /* show some info about interaction parameters */
            final android.view.ViewConfiguration Config = android.view.ViewConfiguration.get(this);
            System.err.printf
              (
                "View config: scaled touch slop = %d, double-tap slop = %d, double-tap timeout = %.3fs, long press timeout = %.3fs\n",
                Config.getScaledTouchSlop(),
                Config.getScaledDoubleTapSlop(),
                Config.getDoubleTapTimeout() / 1000.0,
                Config.getLongPressTimeout() / 1000.0
              );
          }
        super.onCreate(SavedInstanceState);
        setContentView(R.layout.main);
        DrawWhat = new SampleDrawer();
        Zoomer = (android.widget.ZoomControls)findViewById(R.id.viewzoomer);
        TheDrawView = (DrawView)findViewById(R.id.drawview);
        TheDrawView.SetDrawer(DrawWhat);
        TheDrawView.SetOnSingleTapListener
          (
            new DrawView.OnTapListener()
              {
                public void OnTap
                  (
                    DrawView TheDrawView,
                    android.graphics.PointF ViewMouseDown
                  )
                  {
                    final android.graphics.PointF DrawMouseDown = TheDrawView.ViewToDraw(ViewMouseDown);
                    android.widget.Toast.makeText
                      (
                        /*context =*/ Main.this,
                        /*text =*/
                            String.format
                              (
                                "single-tap at (%.1f, %.1f) => %.1f, %.1f)",
                                ViewMouseDown.x, ViewMouseDown.y,
                                DrawMouseDown.x, DrawMouseDown.y
                              ),
                        /*duration =*/ android.widget.Toast.LENGTH_SHORT
                      ).show();
                  } /*onTap*/
              } /*DrawView.OnTapListener*/
          );
        TheDrawView.SetOnDoubleTapListener
          (
            new DrawView.OnTapListener()
              {
                public void OnTap
                  (
                    DrawView TheDrawView,
                    android.graphics.PointF Where
                  )
                  {
                  /* move point that user tapped to centre of view if possible */
                    TheDrawView.ScrollTo
                      (
                        TheDrawView.FindScrollOffset
                          (
                            /*DrawCoords =*/ TheDrawView.ViewToDraw(Where),
                            /*ViewCoords =*/
                                new android.graphics.PointF
                                  (
                                    TheDrawView.getWidth() / 2.0f,
                                    TheDrawView.getHeight() / 2.0f
                                  ),
                            /*ZoomFactor =*/ TheDrawView.GetZoomFactor()
                          ),
                        true
                      );
                  } /*onTap*/
              } /*DrawView.OnTapListener*/
          );
        TheDrawView.SetContextMenuAction
          (
            new DrawView.ContextMenuAction()
              {
                public void CreateContextMenu
                  (
                    android.view.ContextMenu TheMenu,
                    android.graphics.PointF ViewMouseDown
                  )
                  {
                    final android.graphics.PointF DrawMouseDown = TheDrawView.ViewToDraw(ViewMouseDown);
                    android.widget.Toast.makeText
                      (
                        /*context =*/ Main.this,
                        /*text =*/
                            String.format
                              (
                                "long-click at (%.1f, %.1f) => %.1f, %.1f)",
                                ViewMouseDown.x, ViewMouseDown.y,
                                DrawMouseDown.x, DrawMouseDown.y
                              ),
                        /*duration =*/ android.widget.Toast.LENGTH_SHORT
                      ).show();
                  } /*CreateContextMenu*/
              } /*DrawView.ContextMenuAction*/
          );
        Zoomer.setOnZoomInClickListener
          (
            new android.view.View.OnClickListener()
              {
                @Override
                public void onClick
                  (
                    android.view.View TheZoomButton
                  )
                  {
                    TheDrawView.ZoomBy(2.0f);
                  } /*onClick*/
              } /*OnClickListener*/
          );
        Zoomer.setOnZoomOutClickListener
          (
            new android.view.View.OnClickListener()
              {
                @Override
                public void onClick
                  (
                    android.view.View TheZoomButton
                  )
                  {
                    TheDrawView.ZoomBy(0.5f);
                  } /*onClick*/
              } /*OnClickListener*/
          );
      } /*onCreate*/

    @Override
    public void onPause()
      {
        super.onPause();
      /* try to avoid “java.lang.OutOfMemoryError: bitmap size exceeds VM budget”
        crashes ... sigh */
        TheDrawView.ForgetViewCache();
      } /*onPause*/

    @Override
    public boolean dispatchKeyEvent
      (
        android.view.KeyEvent TheEvent
      )
      {
        boolean Handled = false;
        if
          (
                TheDrawView != null
            &&
                TheEvent.getAction() == android.view.KeyEvent.ACTION_UP
            &&
                TheEvent.getKeyCode() == android.view.KeyEvent.KEYCODE_MENU
          )
          {
            new ControlDialog(this).show();
            Handled = true;
          } /*if*/
        if (!Handled)
          {
            Handled = super.dispatchKeyEvent(TheEvent);
          } /*if*/
        return
            Handled;
      } /*dispatchKeyEvent*/

  } /*Main*/
