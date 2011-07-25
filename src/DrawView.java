package nz.gen.geek_central.view_cache_demo;
/*
    Demonstration of how to do smooth scrolling of a complex
    image by careful caching of bitmaps--actual view caching.

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

import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Bitmap;

public class DrawView extends android.view.View
  {
    protected android.content.Context Context;
    protected float ZoomFactor;
    protected float ScrollX, ScrollY; /* [0.0 .. 1.0] */
    protected final float MaxZoomFactor = 32.0f;
    protected final float MinZoomFactor = 1.0f;

    protected Drawer DrawWhat;
    protected boolean UseCaching = true;

    protected static class ViewCacheBits
      {
        public final Bitmap Bits;
        public final RectF Bounds;

        public ViewCacheBits
          (
            Bitmap Bits,
            RectF Bounds
          )
          {
            this.Bits = Bits;
            this.Bounds = Bounds;
          } /*ViewCacheBits*/

      } /*ViewCacheBits*/

    protected PointF LastMouse = null;
    protected final float MaxCacheFactor = 2.0f;
      /* how far to cache beyond visible bounds */
    protected ViewCacheBits ViewCache = null;
    protected ViewCacheBuilder BuildViewCache = null;
    protected boolean CacheRebuildNeeded = false;
    boolean MouseMoved = false;

    protected class ViewCacheBuilder extends android.os.AsyncTask<Void, Integer, ViewCacheBits>
      {
        protected RectF ScaledViewBounds, CacheBounds;

        protected void onPreExecute()
          {
            final ViewParms v = new ViewParms();
            ScaledViewBounds = new RectF(0, 0, v.ScaledViewWidth, v.ScaledViewHeight);
            final PointF ViewOffset = ScrollOffset(v);
            final PointF ViewCenter = new PointF
              (
                /*x =*/ v.ViewWidth / 2.0f - ViewOffset.x,
                /*y =*/ v.ViewHeight / 2.0f - ViewOffset.y
              );
            CacheBounds = new RectF
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
            final Bitmap CacheBits =
                Bitmap.createBitmap
                  (
                    /*width =*/ (int)(CacheBounds.right - CacheBounds.left),
                    /*height =*/ (int)(CacheBounds.bottom - CacheBounds.top),
                    /*config =*/ Bitmap.Config.ARGB_8888
                  );
            final android.graphics.Canvas CacheDraw = new android.graphics.Canvas(CacheBits);
            final RectF DestRect = new RectF(ScaledViewBounds);
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
            DisposeViewCache();
            ViewCache = Result;
            BuildViewCache = null;
            CacheRebuildNeeded = false;
          } /*onPostExecute*/

      } /*ViewCacheBuilder*/

    protected void Init
      (
        android.content.Context Context
      )
      /* common code for all constructors */
      {
        this.Context = Context;
        ZoomFactor = 1.0f;
        ScrollX = 0.5f;
        ScrollY = 0.5f;
        setHorizontalFadingEdgeEnabled(true);
        setVerticalFadingEdgeEnabled(true);
      } /*Init*/

    public DrawView
      (
        android.content.Context Context
      )
      {
        super(Context);
        Init(Context);
      } /*DrawView*/

    public DrawView
      (
        android.content.Context Context,
        android.util.AttributeSet Attributes
      )
      {
        this(Context, Attributes, 0);
      } /*DrawView*/

    public DrawView
      (
        android.content.Context Context,
        android.util.AttributeSet Attributes,
        int DefaultStyle
      )
      {
        super(Context, Attributes, DefaultStyle);
        Init(Context);
      } /*DrawView*/

    public void SetDrawer
      (
        Drawer DrawWhat
      )
      {
        this.DrawWhat = DrawWhat;
      } /*SetDrawer*/

    public boolean GetUseCaching()
      {
        return
            UseCaching;
      } /*GetUseCaching*/

    public void SetUseCaching
      (
        boolean NewUseCaching
      )
      {
        UseCaching = NewUseCaching;
        if (!UseCaching)
          {
            ForgetViewCache();
          } /*if*/
      } /*SetUseCaching*/

    protected void DisposeViewCache()
      {
        if (ViewCache != null)
          {
            ViewCache.Bits.recycle();
            ViewCache = null;
          } /*if*/
      } /*DisposeViewCache*/

    protected void CancelViewCacheBuild()
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

    public void ForgetViewCache()
      {
        CancelViewCacheBuild();
        DisposeViewCache();
      } /*ForgetViewCache*/

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

    protected PointF ScrollOffset
      (
        ViewParms v
      )
      /* returns the amounts by which to offset the scaled view as
        computed from the current scroll values. Note both components
        will be non-positive. */
      {
        return
            new PointF
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
            final PointF ViewOffset = ScrollOffset(v);
            if (ViewCache != null)
              {
              /* cache available, use it */
                final RectF DestRect = new RectF(ViewCache.Bounds);
                DestRect.offset(ViewOffset.x, ViewOffset.y);
              /* Unfortunately, the sample image doesn't look exactly the same
                when drawn offscreen and then copied on-screen, versus being
                drawn directly on-screen: path strokes are slightly thicker
                in the former case. Not sure what to do about this. */
                g.drawBitmap(ViewCache.Bits, null, DestRect, null);
              }
            else
              {
              /* do it the slow way */
                final RectF DestRect = new RectF(0, 0, v.ScaledViewWidth, v.ScaledViewHeight);
                DestRect.offset(ViewOffset.x, ViewOffset.y);
                DrawWhat.Draw(g, DestRect);
                if (UseCaching && BuildViewCache == null)
                  {
                  /* first call, nobody has called RebuildViewCache yet, do it */
                    RebuildViewCache();
                  /* Note, however, that CPU contention can slow down the cache rebuild
                    if the user does a lot of rapid scrolling in the meantime. Would
                    probably work better on multicore. */
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
            LastMouse = new PointF(TheEvent.getX(), TheEvent.getY());
            MouseMoved = false;
            Handled = true;
        break;
        case android.view.MotionEvent.ACTION_MOVE:
            if (LastMouse != null && DrawWhat != null)
              {
                final PointF ThisMouse = new PointF(TheEvent.getX(), TheEvent.getY());
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
                if (Math.hypot(ThisMouse.x - LastMouse.x, ThisMouse.y - LastMouse.y) > 2.0)
                  {
                    MouseMoved = true;
                  } /*if*/
                LastMouse = ThisMouse;
              } /*if*/
            Handled = true;
        break;
        case android.view.MotionEvent.ACTION_UP:
            if (LastMouse != null && !MouseMoved)
              {
              /* move point that user tapped to centre of view if possible */
                final ViewParms v = new ViewParms();
                final PointF ViewOffset = ScrollOffset(v);
                final PointF NewCenter = new PointF
                  (
                    (DrawWhat.Bounds.right + DrawWhat.Bounds.left) / 2.0f,
                    (DrawWhat.Bounds.bottom + DrawWhat.Bounds.top) / 2.0f
                  );
                if (v.ScaledViewWidth > v.ViewWidth)
                  {
                    NewCenter.x =
                                (LastMouse.x - ViewOffset.x)
                           /
                                v.ScaledViewWidth
                           *
                                v.DrawWidth
                        +
                            DrawWhat.Bounds.left;
                  } /*if*/
                if (v.ScaledViewHeight > v.ViewHeight)
                  {
                    NewCenter.y =
                                (LastMouse.y - ViewOffset.y)
                           /
                                v.ScaledViewHeight
                           *
                                v.DrawHeight
                        +
                            DrawWhat.Bounds.top;
                  } /*if*/
                ScrollTo(NewCenter.x, NewCenter.y);
              } /*if*/
            LastMouse = null;
            if (CacheRebuildNeeded && BuildViewCache == null)
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
            DisposeViewCache();
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
                            (X - DrawWhat.Bounds.left) / v.DrawWidth * v.ScaledViewWidth
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
                            (Y - DrawWhat.Bounds.top) / v.DrawHeight * v.ScaledViewHeight
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
        CacheRebuildNeeded = UseCaching;
        super.invalidate();
      } /*NoCacheInvalidate*/

    @Override
    public void invalidate()
      {
        if (DrawWhat != null)
          {
            DisposeViewCache();
              /* because redraw might happen before cache generation is complete */
            if (UseCaching)
              {
                RebuildViewCache();
              } /*if*/
          } /*if*/
        super.invalidate();
      } /*invalidate*/

  /* implementing the following (and calling setxxxFadingEdgeEnabled(true)
    in the constructors, above) will cause fading edges to appear */

    protected static final int ScrollScale = 1000;

    @Override
    protected int computeHorizontalScrollExtent()
      {
        final ViewParms v = new ViewParms();
        return
            (int)Math.round(v.ViewWidth * ScrollScale / v.ScaledViewWidth);
      } /*computeHorizontalScrollExtent*/

    @Override
    protected int computeHorizontalScrollOffset()
      {
        final ViewParms v = new ViewParms();
        return
            v.ScaledViewWidth > v.ViewWidth ?
                (int)Math.round(ScrollX * ScrollScale * (v.ScaledViewWidth - v.ViewWidth) /  v.ScaledViewWidth)
            :
                0;
      } /*computeHorizontalScrollOffset*/

    @Override
    protected int computeHorizontalScrollRange()
      {
        return
            ScrollScale;
      } /*computeHorizontalScrollRange*/

    @Override
    protected int computeVerticalScrollExtent()
      {
        final ViewParms v = new ViewParms();
        return
            (int)Math.round(v.ViewHeight * ScrollScale / v.ScaledViewHeight);
      } /*computeVerticalScrollExtent*/

    @Override
    protected int computeVerticalScrollOffset()
      {
        final ViewParms v = new ViewParms();
        return
            v.ScaledViewHeight > v.ViewHeight ?
                (int)Math.round(ScrollY * ScrollScale * (v.ScaledViewHeight - v.ViewHeight) / v.ScaledViewHeight)
            :
                0;
      } /*computeVerticalScrollOffset*/

    @Override
    protected int computeVerticalScrollRange()
      {
        return
            ScrollScale;
      } /*computeVerticalScrollRange*/

  } /*DrawView*/
