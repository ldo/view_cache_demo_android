package nz.gen.geek_central.view_cache_demo;
/*
    Demonstration of how to do smooth scrolling of a complex
    image by careful caching of bitmaps--app mainline.

    Copyright 2011 by Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.

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

public class Main extends android.app.Activity
  {
    protected android.view.ViewGroup MainLayout;
    protected android.widget.ZoomControls Zoomer;
    protected Drawer DrawWhat;
    protected DrawView TheDrawView;

    protected class ControlDialog
        extends android.app.Dialog
        implements android.content.DialogInterface.OnDismissListener
      {
        final android.content.Context ctx;
        android.widget.RadioGroup TheButtons;

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
            setTitle(R.string.cache_control);
            final android.widget.LinearLayout MainLayout = new android.widget.LinearLayout(ctx);
            MainLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            setContentView(MainLayout);
            TheButtons = new android.widget.RadioGroup(ctx);
            final android.view.ViewGroup.LayoutParams ButtonLayout =
                new android.view.ViewGroup.LayoutParams
                  (
                    android.view.ViewGroup.LayoutParams.FILL_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                  );
              {
                final android.widget.RadioButton CachingOn =
                    new android.widget.RadioButton(ctx);
                CachingOn.setText(R.string.on);
                CachingOn.setId(1);
                final android.widget.RadioButton CachingOff =
                    new android.widget.RadioButton(ctx);
                CachingOff.setText(R.string.off);
                CachingOff.setId(0);
                TheButtons.addView(CachingOn, 0, ButtonLayout);
                TheButtons.addView(CachingOff, 1, ButtonLayout);
              }
            MainLayout.addView(TheButtons, ButtonLayout);
            TheButtons.check(TheDrawView.GetUseCaching() ? 1 : 0);
            setOnDismissListener(this);
          } /*onCreate*/

        @Override
        public void onDismiss
          (
            android.content.DialogInterface TheDialog
          )
          {
            TheDrawView.SetUseCaching(TheButtons.getCheckedRadioButtonId() != 0);
          } /*onClick*/

      } /*ControlDialog*/

    @Override
    public void onCreate
      (
        android.os.Bundle SavedInstanceState
      )
      {
        super.onCreate(SavedInstanceState);
        setContentView(R.layout.main);
        DrawWhat = new Drawer();
        Zoomer = (android.widget.ZoomControls)findViewById(R.id.viewzoomer);
        TheDrawView = (DrawView)findViewById(R.id.drawview);
        TheDrawView.SetDrawer(DrawWhat);
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
