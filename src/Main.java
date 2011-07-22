package nz.gen.geek_central.view_cache_demo;
/*
    Demonstration of how to do smooth scrolling of a complex
    image by careful caching of bitmaps--app mainline.

    Written by Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.
*/

public class Main extends android.app.Activity
  {
    protected android.view.ViewGroup MainLayout;
    protected android.widget.ZoomControls Zoomer;
    protected Drawer DrawWhat;
    protected DrawView TheDrawView;

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
        TheDrawView.CancelViewCacheBuild();
        TheDrawView.ForgetViewCache();
      } /*onPause*/

  } /*Main*/
