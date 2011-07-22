package nz.gen.geek_central.view_cache_demo;
/*
    Demonstration of how to do smooth scrolling of a complex
    image by careful caching of bitmaps--actual view caching.

    Written by Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.
*/

public class DrawView extends android.view.View
  {
    protected android.content.Context Context;
    protected float ZoomFactor;
    protected float ScrollX, ScrollY; /* [0.0 .. 1.0] */
    protected final float MaxZoomFactor = 32.0f;
    protected final float MinZoomFactor = 1.0f;

    protected Drawer DrawWhat;

    protected static class ViewCacheBits
      {
        public final android.graphics.Bitmap Bits;
        public final android.graphics.RectF Bounds;

        public ViewCacheBits
          (
            android.graphics.Bitmap Bits,
            android.graphics.RectF Bounds
          )
          {
            this.Bits = Bits;
            this.Bounds = Bounds;
          } /*ViewCacheBits*/

      } /*ViewCacheBits*/

    protected android.graphics.PointF LastMouse = null;
    protected final float MaxCacheFactor = 2.0f;
    protected ViewCacheBits ViewCache = null;
    protected ViewCacheBuilder BuildViewCache = null;
    protected boolean CacheRebuildNeeded = false;

    protected class ViewCacheBuilder extends android.os.AsyncTask<Void, Integer, ViewCacheBits>
      {
        protected android.graphics.RectF ScaledViewBounds, CacheBounds;

        protected void onPreExecute()
          {
            final ViewParms v = new ViewParms();
            ScaledViewBounds =
                new android.graphics.RectF(0, 0, v.ScaledViewWidth, v.ScaledViewHeight);
            final android.graphics.PointF ViewOffset = ScrollOffset(v);
            final android.graphics.PointF ViewCenter = new android.graphics.PointF
              (
                /*x =*/ v.ViewWidth / 2.0f - ViewOffset.x,
                /*y =*/ v.ViewHeight / 2.0f - ViewOffset.y
              );
            CacheBounds = new android.graphics.RectF
              (
                /*left =*/
                    Math.max
                      (
                        ViewCenter.x - v.ViewWidth * MaxCacheFactor / 2.0f,
                        ScaledViewBounds.left
                      ),
                /*top =*/
                    Math.max
                      (
                        ViewCenter.y - v.ViewHeight * MaxCacheFactor / 2.0f,
                        ScaledViewBounds.top
                      ),
                /*right =*/
                    Math.min
                      (
                        ViewCenter.x + v.ViewWidth * MaxCacheFactor / 2.0f,
                        ScaledViewBounds.right
                      ),
                /*bottom =*/
                    Math.min
                      (
                        ViewCenter.y + v.ViewHeight * MaxCacheFactor / 2.0f,
                        ScaledViewBounds.bottom
                      )
              );
            if (CacheBounds.isEmpty())
              {
              /* can seem to happen, e.g. on orientation change */
                cancel(true);
              } /*if*/
          } /*onPreExecute*/

        protected ViewCacheBits doInBackground
          (
            Void... Unused
          )
          {
            final android.graphics.Bitmap CacheBits =
                android.graphics.Bitmap.createBitmap
                  (
                    /*width =*/ (int)(CacheBounds.right - CacheBounds.left),
                    /*height =*/ (int)(CacheBounds.bottom - CacheBounds.top),
                    /*config =*/ android.graphics.Bitmap.Config.ARGB_8888
                  );
            final android.graphics.Canvas CacheDraw = new android.graphics.Canvas(CacheBits);
            final android.graphics.RectF DestRect = new android.graphics.RectF(ScaledViewBounds);
            DestRect.offset(- CacheBounds.left, - CacheBounds.top);
            DrawWhat.Draw(CacheDraw, DestRect); /* this is the time-consuming part */
            CacheBits.prepareToDraw();
            return
                new ViewCacheBits(CacheBits, CacheBounds);
          } /*doInBackground*/

        protected void onCancelled
          (
            ViewCacheBits Result
          )
          {
            Result.Bits.recycle();
          } /*onCancelled*/

        protected void onPostExecute
          (
            ViewCacheBits Result
          )
          {
            ForgetViewCache();
            ViewCache = Result;
            BuildViewCache = null;
            CacheRebuildNeeded = false;
          } /*onPostExecute*/

      } /*ViewCacheBuilder*/

    public DrawView
      (
        android.content.Context Context,
        android.util.AttributeSet Attributes
      )
      {
        super(Context, Attributes);
        this.Context = Context;
        ZoomFactor = 1.0f;
        ScrollX = 0.5f;
        ScrollY = 0.5f;
      } /*DrawView*/

    public void SetDrawer
      (
        Drawer DrawWhat
      )
      {
        this.DrawWhat = DrawWhat;
      } /*SetDrawer*/

    public void ForgetViewCache()
      {
        if (ViewCache != null)
          {
            ViewCache.Bits.recycle();
            ViewCache = null;
          } /*if*/
      } /*ForgetViewCache*/

    public void CancelViewCacheBuild()
      {
        if (BuildViewCache != null)
          {
            BuildViewCache.cancel
              (
                false
                  /* not true to allow onCancelled to recycle bitmap */
              );
            BuildViewCache = null;
          } /*if*/
      } /*CancelViewCacheBuild*/

    protected void RebuildViewCache()
      {
        CancelViewCacheBuild();
        BuildViewCache = new ViewCacheBuilder();
        BuildViewCache.execute((Void)null);
      } /*RebuildViewCache*/

    protected class ViewParms
      /* parameters for scaling and positioning map display */
      {
        public final float DrawWidth, DrawHeight;
        public final float ViewWidth, ViewHeight;
        public float ScaledViewWidth, ScaledViewHeight;

        public ViewParms
          (
            float ZoomFactor
          )
          {
            DrawWidth = DrawWhat.Bounds.right - DrawWhat.Bounds.left;
            DrawHeight = DrawWhat.Bounds.bottom - DrawWhat.Bounds.top;
            ViewWidth = getWidth();
            ViewHeight = getHeight();
            ScaledViewWidth = ViewWidth * ZoomFactor;
            ScaledViewHeight = ViewHeight * ZoomFactor;
            if (ScaledViewWidth > ScaledViewHeight * DrawWidth / DrawHeight)
              {
                ScaledViewWidth = ScaledViewHeight * DrawWidth / DrawHeight;
              }
            else if (ScaledViewHeight > ScaledViewWidth * DrawHeight / DrawWidth)
              {
                ScaledViewHeight = ScaledViewWidth * DrawHeight / DrawWidth;
              } /*if*/
          } /*ViewParms*/

        public ViewParms()
          {
            this(DrawView.this.ZoomFactor);
          } /*ViewParms*/

      } /*ViewParms*/

    android.graphics.PointF ScrollOffset
      (
        ViewParms v
      )
      /* returns the amounts by which to offset the scaled view as
        computed from the current scroll values. Note both components
        will be non-positive. */
      {
        return
            new android.graphics.PointF
              (
                /*x =*/
                        (v.ViewWidth - v.ScaledViewWidth)
                    *
                        (v.ScaledViewWidth >= v.ViewWidth ? ScrollX : 0.5f),
                /*y =*/
                        (v.ViewHeight - v.ScaledViewHeight)
                    *
                        (v.ScaledViewHeight >= v.ViewHeight ? ScrollY : 0.5f)
              );
      } /*ScrollOffset*/

    @Override
    public void onDraw
      (
        android.graphics.Canvas g
      )
      {
        if (DrawWhat != null)
          {
            final ViewParms v = new ViewParms();
            final android.graphics.PointF ViewOffset = ScrollOffset(v);
            if (ViewCache != null)
              {
                final android.graphics.RectF DestRect = new android.graphics.RectF(ViewCache.Bounds);
                DestRect.offset(ViewOffset.x, ViewOffset.y);
                g.drawBitmap(ViewCache.Bits, null, DestRect, null);
              }
            else
              {
                final android.graphics.RectF DestRect =
                    new android.graphics.RectF(0, 0, v.ScaledViewWidth, v.ScaledViewHeight);
                DestRect.offset(ViewOffset.x, ViewOffset.y);
                DrawWhat.Draw(g, DestRect);
                if (BuildViewCache == null)
                  {
                  /* first call, nobody has called RebuildViewCache yet, do it */
                    RebuildViewCache();
                  } /*if*/
              } /*if*/
          } /*if*/
      } /*onDraw*/

    @Override
    public boolean onTouchEvent
      (
        android.view.MotionEvent TheEvent
      )
      {
        boolean Handled = false;
      /* multi-touch TBD */
        switch (TheEvent.getAction())
          {
        case android.view.MotionEvent.ACTION_DOWN:
            LastMouse = new android.graphics.PointF(TheEvent.getX(), TheEvent.getY());
            Handled = true;
        break;
        case android.view.MotionEvent.ACTION_MOVE:
            if (LastMouse != null && DrawWhat != null)
              {
                final android.graphics.PointF ThisMouse =
                    new android.graphics.PointF(TheEvent.getX(), TheEvent.getY());
                final ViewParms v = new ViewParms();
                if (v.ScaledViewWidth > v.ViewWidth && ThisMouse.x != LastMouse.x)
                  {
                    final float ScrollDelta =
                        (ThisMouse.x - LastMouse.x) / (v.ViewWidth - v.ScaledViewWidth);
                    ScrollX = Math.max(0.0f, Math.min(1.0f, ScrollX + ScrollDelta));
                    NoCacheInvalidate();
                  } /*if*/
                if (v.ScaledViewHeight > v.ViewHeight && ThisMouse.y != LastMouse.y)
                  {
                    final float ScrollDelta =
                        (ThisMouse.y - LastMouse.y) / (v.ViewHeight - v.ScaledViewHeight);
                    ScrollY = Math.max(0.0f, Math.min(1.0f, ScrollY + ScrollDelta));
                    NoCacheInvalidate();
                  } /*if*/
                LastMouse = ThisMouse;
              } /*if*/
            Handled = true;
        break;
        case android.view.MotionEvent.ACTION_UP:
            LastMouse = null;
            if (CacheRebuildNeeded)
              {
                invalidate();
              } /*if*/
            Handled = true;
        break;
          } /*switch*/
        return
            Handled;
      } /*onTouchEvent*/

    public void ZoomBy
      (
        float Factor
      )
      /* multiplies the current zoom by Factor. */
      {
        final float NewZoomFactor =
            Math.min
              (
                Math.max
                  (
                    ZoomFactor * Math.abs(Factor),
                    MinZoomFactor
                  ),
                MaxZoomFactor
              );
        if (NewZoomFactor != ZoomFactor)
          {
            ForgetViewCache();
          /* try to adjust scroll offset so point in map at centre of view stays in centre */
            final ViewParms v1 = new ViewParms();
            final ViewParms v2 = new ViewParms(NewZoomFactor);
            if (v1.ScaledViewWidth > v1.ViewWidth && v2.ScaledViewWidth > v2.ViewWidth)
              {
                ScrollX =
                        (
                                (
                                    v1.ViewWidth / 2.0f
                                +
                                    ScrollX * (v1.ScaledViewWidth - v1.ViewWidth)
                                )
                            /
                                v1.ScaledViewWidth
                            *
                                v2.ScaledViewWidth
                        -
                            v2.ViewWidth / 2.0f
                        )
                    /
                        (v2.ScaledViewWidth - v2.ViewWidth);
              } /*if*/
            if (v1.ScaledViewHeight > v1.ViewHeight && v2.ScaledViewHeight > v2.ViewHeight)
              {
                ScrollY =
                        (
                                (
                                    v1.ViewHeight / 2.0f
                                +
                                    ScrollY * (v1.ScaledViewHeight - v1.ViewHeight)
                                )
                            /
                                v1.ScaledViewHeight
                            *
                                v2.ScaledViewHeight
                        -
                            v2.ViewHeight / 2.0f
                        )
                    /
                        (v2.ScaledViewHeight - v2.ViewHeight);
              } /*if*/
            ZoomFactor = NewZoomFactor;
            invalidate();
          } /*if*/
      } /*ZoomBy*/

    public void ScrollTo
      (
        float X,
        float Y
      )
      /* tries to ensure the specified position is at the centre of the view. */
      {
        if (DrawWhat != null)
          {
            final ViewParms v = new ViewParms();
            final float OldScrollX = ScrollX;
            final float OldScrollY = ScrollY;
            if (v.ScaledViewWidth > v.ViewWidth)
              {
                ScrollX =
                        (
                                (X - DrawWhat.Bounds.left)
                            /
                                v.DrawWidth
                            *
                                v.ScaledViewWidth
                        -
                            v.ViewWidth / 2.0f
                        )
                    /
                        (v.ScaledViewWidth - v.ViewWidth);
                ScrollX = Math.max(0.0f, Math.min(1.0f, ScrollX));
              } /*if*/
            if (v.ScaledViewHeight > v.ViewHeight)
              {
                ScrollY =
                        (
                            (DrawWhat.Bounds.top - Y) / v.DrawHeight * v.ScaledViewHeight
                        -
                            v.ViewHeight / 2.0f
                        )
                    /
                        (v.ScaledViewHeight - v.ViewHeight);
                ScrollY = Math.max(0.0f, Math.min(1.0f, ScrollY));
              } /*if*/
            if (OldScrollX != ScrollX || OldScrollY != ScrollY)
              {
                invalidate();
              } /*if*/
          } /*if*/
      } /*ScrollTo*/

    protected void NoCacheInvalidate()
      /* try to avoid “java.lang.OutOfMemoryError: bitmap size exceeds VM budget”
        crashes by minimizing cache rebuild calls. */
      {
        CacheRebuildNeeded = true;
        super.invalidate();
      } /*NoCacheInvalidate*/

    @Override
    public void invalidate()
      {
        if (DrawWhat != null)
          {
            ForgetViewCache(); /* because redraw might happen before cache generation is complete */
            RebuildViewCache();
          } /*if*/
        super.invalidate();
      } /*invalidate*/

  } /*DrawView*/
