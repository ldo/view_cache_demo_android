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
import android.view.MotionEvent;

public class DrawView extends android.view.View
  {
    protected android.content.Context Context;
    protected float ZoomFactor;
    protected PointF ScrollOffset;
      /* range along each axis is [0.0 .. 1.0], such that 0
        corresponds to left/top edge of image being at left/top edge
        of view, 0.5 corresponds to middle of image being in middle of
        view, and 1 corresponds to right/bottom edge of image being at
        right/bottom edge of view */
    protected final float MaxZoomFactor = 32.0f;
    protected final float MinZoomFactor = 1.0f;

    protected Drawer DrawWhat;
    protected boolean UseCaching = true;

    protected void Init
      (
        android.content.Context Context
      )
      /* common code for all constructors */
      {
        this.Context = Context;
        ScrollOffset = new PointF(0.5f, 0.5f);
        ZoomFactor = 1.0f;
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

/*
    Mapping between image coordinates and view coordinates
*/

    static PointF MapRect
      (
        PointF Pt,
        RectF Src,
        RectF Dst
      )
      /* maps Pt from Src coordinates to Dst coordinates. */
      {
        return
            new PointF
              (
                        (Pt.x - Src.left)
                    /
                        (Src.right - Src.left)
                    *
                        (Dst.right - Dst.left)
                +
                    Dst.left,
                        (Pt.y - Src.top)
                    /
                        (Src.bottom - Src.top)
                    *
                        (Dst.bottom - Dst.top)
                +
                    Dst.top
              );
      } /*MapRect*/

    protected PointF GetViewSize
      (
        float ZoomFactor
      )
      /* gets the image bounds in view coordinates, adjusted for the specified
        zoom setting. */
      {
        final RectF DrawBounds = DrawWhat.GetBounds();
        final PointF ViewSize = new PointF(getWidth() * ZoomFactor, getHeight() * ZoomFactor);
        if
          (
                ViewSize.x / ViewSize.y
            >
                (DrawBounds.right - DrawBounds.left) / (DrawBounds.bottom - DrawBounds.top)
          )
          {
          /* leave unused margin at right of too-wide view */
            ViewSize.x =
                    ViewSize.y
                *
                    (DrawBounds.right - DrawBounds.left)
                /
                    (DrawBounds.bottom - DrawBounds.top);
          }
        else
          {
          /* leave unused margin at bottom of too-tall view, if any */
            ViewSize.y =
                    ViewSize.x
                *
                    (DrawBounds.bottom - DrawBounds.top)
                /
                    (DrawBounds.right - DrawBounds.left);
          } /*if*/
        return
            ViewSize;
      } /*GetViewSize*/

    protected PointF GetViewSize()
      /* gets the image bounds in view coordinates, adjusted for the current
        zoom setting. */
      {
        return
            GetViewSize(ZoomFactor);
      } /*GetViewSize*/

    protected RectF GetViewBounds
      (
        float ZoomFactor
      )
      {
        final PointF ViewSize = GetViewSize(ZoomFactor);
        return
            new RectF(0.0f, 0.0f, ViewSize.x, ViewSize.y);
      } /*GetViewBounds*/

    protected RectF GetScrolledViewBounds
      (
        PointF ScrollOffset,
        float ZoomFactor
      )
      /* gets the image bounds in view coordinates, adjusted for the specified
        scroll offset and zoom setting. */
      {
        final PointF ViewSize = GetViewSize(ZoomFactor);
        final RectF ViewBounds = GetViewBounds(ZoomFactor);
        ViewBounds.offset
          (
            /*dx =*/ Math.min(ScrollOffset.x * (getWidth() - ViewSize.x), 0.0f),
            /*dy =*/ Math.min(ScrollOffset.y * (getHeight() - ViewSize.y), 0.0f)
          );
        return
            ViewBounds;
      } /*GetScrolledViewBounds*/

    protected RectF GetScrolledViewBounds()
      /* gets the image bounds in view coordinates, adjusted for the current
        scroll offset and zoom setting. */
      {
        return
            GetScrolledViewBounds(ScrollOffset, ZoomFactor);
      } /*GetScrolledViewBounds*/

    protected PointF DrawToView
      (
        PointF DrawCoords,
        PointF ScrollOffset,
        float ZoomFactor
      )
      /* maps DrawCoords to view coordinates at the specified scroll offset
        and zoom setting. */
      {
        return
            MapRect
              (
                /*Pt =*/ DrawCoords,
                /*Src =*/ DrawWhat.GetBounds(),
                /*Dst =*/ GetScrolledViewBounds(ScrollOffset, ZoomFactor)
              );
      } /*DrawToView*/

    protected PointF DrawToView
      (
        PointF DrawCoords
      )
      /* maps DrawCoords to view coordinates at the current scroll offset
        and zoom setting. */
      {
        return
            MapRect(DrawCoords, DrawWhat.GetBounds(), GetScrolledViewBounds());
      } /*DrawToView*/

    protected PointF ViewToDraw
      (
        PointF ViewCoords,
        PointF ScrollOffset,
        float ZoomFactor
      )
      /* maps ViewCoords to draw coordinates at the specified scroll offset
        and zoom setting. */
      {
        return
            MapRect
              (
                /*Pt =*/ ViewCoords,
                /*Src =*/ GetScrolledViewBounds(ScrollOffset, ZoomFactor),
                /*Dst =*/ DrawWhat.GetBounds()
              );
      } /*ViewToDraw*/

    protected PointF ViewToDraw
      (
        PointF ViewCoords
      )
      /* maps ViewCoords to draw coordinates at the current scroll offset
        and zoom setting. */
      {
        return
            ViewToDraw(ViewCoords, ScrollOffset, ZoomFactor);
      } /*ViewToDraw*/

    protected PointF FindScrollOffset
      (
        PointF DrawCoords,
        PointF ViewCoords,
        float ZoomFactor
      )
      /* computes the necessary scroll offset so DrawCoords maps to ViewCoords
        at the specified zoom setting. */
      {
        final RectF DrawBounds = DrawWhat.GetBounds();
        final PointF ViewSize = GetViewSize(ZoomFactor);
        return
            new PointF
              (
                /*x =*/
                    ViewSize.x != getWidth() ?
                        Math.max
                          (
                            0.0f,
                            Math.min
                              (
                                    (
                                            (DrawCoords.x - DrawBounds.left)
                                        /
                                            (DrawBounds.right - DrawBounds.left)
                                        *
                                            ViewSize.x
                                    -
                                        ViewCoords.x
                                    )
                                /
                                    (ViewSize.x - getWidth()),
                                1.0f
                              )
                          )
                    :
                        0.5f,
                /*y =*/
                    ViewSize.y != getHeight() ?
                        Math.max
                          (
                            0.0f,
                            Math.min
                              (
                                    (
                                            (DrawCoords.y - DrawBounds.top)
                                        /
                                            (DrawBounds.bottom - DrawBounds.top)
                                        *
                                            ViewSize.y
                                    -
                                        ViewCoords.y
                                    )
                                /
                                    (ViewSize.y - getHeight()),
                                1.0f
                              )
                          )
                    :
                        0.5f
              );
      } /*FindScrollOffset*/

/*
    View cache management & drawing
*/

    protected static class ViewCacheBits
      /* state of the view cache */
      {
        public final Bitmap Bits; /* cached part of image */
        public final RectF Bounds;
          /* such that (0, 0) maps to (DrawWhat.GetBounds().left, DrawWhat.GetBounds().top)
            but scaled to view bounds at current zoom */

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

    protected final float MaxCacheFactor = 2.0f;
      /* how far to cache beyond visible bounds, relative to the bounds */
    protected ViewCacheBits ViewCache = null;
    protected ViewCacheBuilder BuildViewCache = null;

    protected class ViewCacheBuilder extends android.os.AsyncTask<Void, Integer, ViewCacheBits>
      /* background rebuilding of the view cache */
      {
        protected PointF ViewSize;
        protected RectF CacheBounds;

        protected void onPreExecute()
          {
            ViewSize = GetViewSize();
            final RectF ViewBounds = GetScrolledViewBounds();
            final PointF ViewCenter = new PointF(getWidth() / 2.0f, getHeight() / 2.0f);
            CacheBounds = new RectF
              (
                /*left =*/
                    Math.max
                      (
                        ViewCenter.x - getWidth() * MaxCacheFactor / 2.0f - ViewBounds.left,
                        0.0f
                      ),
                /*top =*/
                    Math.max
                      (
                        ViewCenter.y - getHeight() * MaxCacheFactor / 2.0f - ViewBounds.top,
                        0.0f
                      ),
                /*right =*/
                    Math.min
                      (
                        ViewCenter.x + getWidth() * MaxCacheFactor / 2.0f - ViewBounds.left,
                        ViewBounds.right - ViewBounds.left
                      ),
                /*bottom =*/
                    Math.min
                      (
                        ViewCenter.y + getHeight() * MaxCacheFactor / 2.0f - ViewBounds.top,
                        ViewBounds.bottom - ViewBounds.top
                      )
              );
            System.err.printf
              (
                "Build cache centre (%.2f, %.2f) <= (%.2f, %.2f) bounds (%.2f, %.2f, %.2f, %.2f) <= (%.2f, %.2f, %.2f, %.2f)\n",
                ViewCenter.x, ViewCenter.y,
                ScrollOffset.x, ScrollOffset.y,
                CacheBounds.left, CacheBounds.top, CacheBounds.right, CacheBounds.bottom,
                ViewBounds.left, ViewBounds.top, ViewBounds.right, ViewBounds.bottom
              ); /* debug */
            if (CacheBounds.isEmpty())
              {
              /* can seem to happen, e.g. on orientation change */
                BuildViewCache = null;
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
            final RectF DestRect = new RectF(0.0f, 0.0f, ViewSize.x, ViewSize.y);
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
            invalidate();
          } /*onPostExecute*/

      } /*ViewCacheBuilder*/

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

    protected void RecenterViewCache()
      /* regenerates the cache as necessary to ensure it completely covers
        currently-visible view. */
      {
        if
          (
                UseCaching
            &&
                BuildViewCache == null
            &&
                ViewCache != null
          )
          {
            final RectF ViewBounds = GetScrolledViewBounds();
            final RectF DestRect = new RectF(ViewCache.Bounds);
            DestRect.offset(ViewBounds.left, ViewBounds.top);
            if
              (
                    DestRect.left > Math.max(0, ViewBounds.left)
                ||
                    DestRect.top > Math.max(0, ViewBounds.top)
                ||
                    DestRect.right < Math.min(getWidth(), ViewBounds.right)
                ||
                    DestRect.bottom < Math.min(getHeight(), ViewBounds.bottom)
              )
              {
              /* cache doesn't completely cover visible part of image */
                System.err.printf
                  (
                    "Cache bounds (%.2f, %.2f, %.2f, %.2f) => (%.2f, %.2f, %.2f, %.2f) don't completely cover view (0, 0, %d, %d) => (%.2f, %.2f, %.2f, %.2f)\n",
                    ViewCache.Bounds.left, ViewCache.Bounds.top, ViewCache.Bounds.right, ViewCache.Bounds.bottom,
                    DestRect.left, DestRect.top, DestRect.right, DestRect.bottom,
                    getWidth(), getHeight(),
                    ViewBounds.left, ViewBounds.top, ViewBounds.right, ViewBounds.bottom
                  ); /* debug */
                RebuildViewCache();
              } /*if*/
          } /*if*/
      } /*RecenterViewCache*/

    @Override
    public void onDraw
      (
        android.graphics.Canvas g
      )
      {
        if (DrawWhat != null)
          {
            final RectF ViewBounds = GetScrolledViewBounds();
            if (ViewCache != null)
              {
              /* cache available, use it */
                final RectF DestRect = new RectF(ViewCache.Bounds);
                System.err.printf
                  (
                    "onDraw cached Scroll = (%.2f, %.2f), ViewBounds = (%.2f, %.2f, %.2f, %.2f), cache bounds = (%.2f, %.2f, %.2f, %.2f)",
                    ScrollOffset.x, ScrollOffset.y,
                    ViewBounds.left, ViewBounds.top, ViewBounds.right, ViewBounds.bottom,
                    DestRect.left, DestRect.top, DestRect.right, DestRect.bottom
                  ); /* debug */
                DestRect.offset(ViewBounds.left, ViewBounds.top);
                System.err.printf
                  (
                    " => (%.2f, %.2f, %.2f, %.2f)\n",
                    DestRect.left, DestRect.top, DestRect.right, DestRect.bottom
                  ); /* debug */
              /* Unfortunately, the sample image doesn't look exactly the same
                when drawn offscreen and then copied on-screen, versus being
                drawn directly on-screen: path strokes are slightly thicker
                in the former case. Not sure what to do about this. */
                g.drawBitmap(ViewCache.Bits, null, DestRect, null);
                RecenterViewCache();
              }
            else if (BuildViewCache != null)
              {
              /* cache rebuild in progress, wait for it to finish before actually drawing,
                to avoid CPU contention that slows things down */
              }
            else
              {
              /* do it the slow way */
                System.err.printf
                  (
                    "onDraw uncached Scroll = (%.2f, %.2f), ViewBounds = (%.2f, %.2f, %.2f, %.2f)\n",
                    ScrollOffset.x, ScrollOffset.y,
                    ViewBounds.left, ViewBounds.top, ViewBounds.right, ViewBounds.bottom
                  ); /* debug */
                DrawWhat.Draw(g, ViewBounds);
                if (UseCaching && BuildViewCache == null)
                  {
                  /* first call, nobody has called RebuildViewCache yet, do it */
                    RebuildViewCache();
                  } /*if*/
              } /*if*/
          } /*if*/
      } /*onDraw*/

    @Override
    public void invalidate()
      {
        RecenterViewCache();
        super.invalidate();
      } /*invalidate*/

/*
    Interaction handling
*/

    protected PointF
        LastMouse1 = null,
        LastMouse2 = null;
    protected int
        Mouse1ID = -1,
        Mouse2ID = -1;
    protected boolean MouseMoved = false;

    protected class ScrollAnimator implements Runnable
      {
        final android.view.animation.Interpolator AnimFunction;
        final double StartTime, EndTime;
        final PointF StartScroll, EndScroll;

        public ScrollAnimator
          (
            android.view.animation.Interpolator AnimFunction,
            double StartTime,
            double EndTime,
            PointF StartScroll,
            PointF EndScroll
          )
          {
            this.AnimFunction = AnimFunction;
            this.StartTime = StartTime;
            this.EndTime = EndTime;
            this.StartScroll = StartScroll;
            this.EndScroll = EndScroll;
            CurrentAnim = this;
            getHandler().post(this);
          } /*ScrollAnimator*/

        public void run()
          {
            if (CurrentAnim == this)
              {
                final double CurrentTime = System.currentTimeMillis() / 1000.0;
                final float AnimAmt =
                    AnimFunction.getInterpolation
                      (
                        (float)((CurrentTime - StartTime) / (EndTime - StartTime))
                      );
                ScrollTo
                  (
                    new PointF
                      (
                        StartScroll.x + (EndScroll.x - StartScroll.x) * AnimAmt,
                        StartScroll.y + (EndScroll.y - StartScroll.y) * AnimAmt
                      )
                  );
                final android.os.Handler MyHandler = getHandler();
                if (MyHandler != null && CurrentTime < EndTime)
                  {
                    MyHandler.post(this);
                  }
                else
                  {
                    CurrentAnim = null;
                  } /*if*/
              } /*if*/
          } /*run*/

      } /*ScrollAnimator*/

    private ScrollAnimator CurrentAnim = null;

    protected final android.view.GestureDetector FlingDetector =
        new android.view.GestureDetector
          (
            Context,
            new android.view.GestureDetector.SimpleOnGestureListener()
              {
                @Override
                public boolean onFling
                  (
                    MotionEvent DownEvent,
                    MotionEvent UpEvent,
                    float XVelocity,
                    float YVelocity
                  )
                  {
                    final RectF ViewBounds = GetScrolledViewBounds();
                    final boolean DoFling =
                            ViewBounds.left < 0 && XVelocity > 0
                        ||
                            ViewBounds.right > getWidth() && XVelocity < 0
                        ||
                            ViewBounds.top < 0 && YVelocity > 0
                        ||
                            ViewBounds.bottom > getHeight() && YVelocity < 0;
                    if (DoFling)
                      {
                        final double CurrentTime = System.currentTimeMillis() / 1000.0;
                        final float ScrollDuration =
                                (float)Math.hypot(XVelocity, YVelocity)
                            /
                                (float)Math.hypot(getWidth(), getHeight());
                        final float Attenuate = 4.0f;
                        final PointF MouseUp = new PointF(UpEvent.getX(), UpEvent.getY());
                        final PointF EndScroll =
                            FindScrollOffset
                              (
                                /*DrawCoords =*/ ViewToDraw(MouseUp),
                                /*ViewCoords =*/
                                    new PointF
                                      (
                                            MouseUp.x
                                        +
                                                XVelocity
                                            *
                                                ScrollDuration
                                            /
                                                Attenuate,
                                            MouseUp.y
                                        +
                                                YVelocity
                                            *
                                                ScrollDuration
                                            /
                                                Attenuate
                                      ),
                                /*ZoomFactor =*/ ZoomFactor
                              );
                        new ScrollAnimator
                          (
                            /*AnimFunction =*/ new android.view.animation.DecelerateInterpolator(),
                            /*StartTime =*/ CurrentTime,
                            /*EndTime =*/ CurrentTime + ScrollDuration,
                            /*StartScroll =*/ ScrollOffset,
                            /*EndScroll =*/ EndScroll
                          );
                      } /*if*/
                    return
                        DoFling;
                  } /*onFling*/
              } /*GestureDetector.SimpleOnGestureListener*/
          );

    @Override
    public boolean onTouchEvent
      (
        MotionEvent TheEvent
      )
      {
        CurrentAnim = null; /* cancel any animation in progress */
        boolean Handled = FlingDetector.onTouchEvent(TheEvent);
        if (!Handled)
          {
            boolean DoRebuild = false;
            switch (TheEvent.getAction() & (1 << MotionEvent.ACTION_POINTER_ID_SHIFT) - 1)
              {
            case MotionEvent.ACTION_DOWN:
                LastMouse1 = new PointF(TheEvent.getX(), TheEvent.getY());
                Mouse1ID = TheEvent.getPointerId(0);
                MouseMoved = false;
                Handled = true;
            break;
            case MotionEvent.ACTION_POINTER_DOWN:
                  {
                    final int PointerIndex =
                            (TheEvent.getAction() & MotionEvent.ACTION_POINTER_ID_MASK)
                        >>
                            MotionEvent.ACTION_POINTER_ID_SHIFT;
                    final int MouseID = TheEvent.getPointerId(PointerIndex);
                    System.err.println("PuzzleView: semi-down pointer ID " + MouseID); /* debug */
                    final PointF MousePos = new PointF
                      (
                        TheEvent.getX(PointerIndex),
                        TheEvent.getY(PointerIndex)
                      );
                    if (LastMouse1 == null)
                      {
                        Mouse1ID = MouseID;
                        LastMouse1 = MousePos;
                      }
                    else if (LastMouse2 == null)
                      {
                        Mouse2ID = MouseID;
                        LastMouse2 = MousePos;
                      } /*if*/
                  }
                Handled = true;
            break;
            case MotionEvent.ACTION_MOVE:
                if (LastMouse1 != null && DrawWhat != null)
                  {
                    final int Mouse1Index = TheEvent.findPointerIndex(Mouse1ID);
                    final int Mouse2Index =
                        LastMouse2 != null ?
                            TheEvent.findPointerIndex(Mouse2ID)
                        :
                            -1;
                    if (Mouse1Index >= 0 || Mouse2Index >= 0)
                      {
                        final PointF ThisMouse1 =
                            Mouse1Index >= 0 ?
                                new PointF
                                  (
                                    TheEvent.getX(Mouse1Index),
                                    TheEvent.getY(Mouse1Index)
                                  )
                            :
                                null;
                        final PointF ThisMouse2 =
                            Mouse2Index >= 0 ?
                                new PointF
                                 (
                                   TheEvent.getX(Mouse2Index),
                                   TheEvent.getY(Mouse2Index)
                                 )
                             :
                                null;
                        if (ThisMouse1 != null || ThisMouse2 != null)
                          {
                            final PointF ThisMouse =
                                ThisMouse1 != null ?
                                    ThisMouse2 != null ?
                                        new PointF
                                          (
                                            (ThisMouse1.x + ThisMouse2.x) / 2.0f,
                                            (ThisMouse1.y + ThisMouse2.y) / 2.0f
                                          )
                                    :
                                        ThisMouse1
                                :
                                    ThisMouse2;
                            final PointF LastMouse =
                                ThisMouse1 != null ?
                                    ThisMouse2 != null ?
                                        new PointF
                                          (
                                            (LastMouse1.x + LastMouse2.x) / 2.0f,
                                            (LastMouse1.y + LastMouse2.y) / 2.0f
                                          )
                                    :
                                        LastMouse1
                                :
                                    LastMouse2;
                            ScrollTo
                              (
                                FindScrollOffset
                                  (
                                    /*DrawCoords =*/ ViewToDraw(LastMouse),
                                    /*ViewCoords =*/ ThisMouse,
                                    /*ZoomFactor =*/ ZoomFactor
                                  )
                              );
                            if
                              (
                                    Math.hypot(ThisMouse.x - LastMouse.x, ThisMouse.y - LastMouse.y)
                                >
                                    2.0
                              )
                              {
                                MouseMoved = true;
                              } /*if*/
                            if (ThisMouse1 != null && ThisMouse2 != null)
                              {
                              /* pinch to zoom */
                                final float LastDistance = (float)Math.hypot
                                  (
                                    LastMouse1.x - LastMouse2.x,
                                    LastMouse1.y - LastMouse2.y
                                  );
                                final float ThisDistance = (float)Math.hypot
                                  (
                                    ThisMouse1.x - ThisMouse2.x,
                                    ThisMouse1.y - ThisMouse2.y
                                  );
                                if
                                  (
                                        LastDistance != 0.0f
                                    &&
                                        ThisDistance != 0.0f
                                  )
                                  {
                                    ZoomBy(ThisDistance /  LastDistance);
                                  } /*if*/
                              } /*if*/
                            LastMouse1 = ThisMouse1;
                            LastMouse2 = ThisMouse2;
                          } /*if*/
                      } /*if*/
                  } /*if*/
                DoRebuild = true;
                Handled = true;
            break;
            case MotionEvent.ACTION_POINTER_UP:
                if (LastMouse2 != null)
                  {
                    final int PointerIndex =
                            (TheEvent.getAction() & MotionEvent.ACTION_POINTER_ID_MASK)
                        >>
                            MotionEvent.ACTION_POINTER_ID_SHIFT;
                    final int PointerID = TheEvent.getPointerId(PointerIndex);
                    System.err.println("PuzzleView: semi-up pointer ID " + PointerID); /* debug */
                    if (PointerID == Mouse1ID)
                      {
                        Mouse1ID = Mouse2ID;
                        LastMouse1 = LastMouse2;
                        Mouse2ID = -1;
                        LastMouse2 = null;
                      }
                    else if (PointerID == Mouse2ID)
                      {
                        Mouse2ID = -1;
                        LastMouse2 = null;
                      } /*if*/
                  } /*if*/
                DoRebuild = true;
                Handled = true;
            break;
            case MotionEvent.ACTION_UP:
                if (LastMouse1 != null && !MouseMoved)
                  {
                  /* move point that user tapped to centre of view if possible */
                    ScrollTo
                      (
                        FindScrollOffset
                          (
                            /*DrawCoords =*/
                                ViewToDraw
                                  (
                                    LastMouse2 != null ?
                                        new PointF
                                          (
                                            (LastMouse1.x + LastMouse2.x) / 2.0f,
                                            (LastMouse1.y + LastMouse2.y) / 2.0f
                                          )
                                    :
                                        LastMouse1
                                  ),
                            /*ViewCoords =*/ new PointF(getWidth() / 2.0f, getHeight() / 2.0f),
                            /*ZoomFactor =*/ ZoomFactor
                          )
                      );
                  } /*if*/
                LastMouse1 = null;
                LastMouse2 = null;
                Mouse1ID = -1;
                Mouse2ID = -1;
                DoRebuild = true;
                Handled = true;
            break;
              } /*switch*/
            if (UseCaching && DoRebuild && BuildViewCache == null)
              {
              /* try to keep cache up to date to minimize appearance of
                black borders in uncached areas during scrolling */
                RebuildViewCache();
              } /*if*/
          } /*if*/
        return
            Handled;
      } /*onTouchEvent*/

/*
    Implementation of saving/restoring instance state. Doing this
    allows me to transparently restore scroll/zoom state if system
    needs to kill me while I'm in the background, or on an orientation
    change while I'm in the foreground.

    Notes: View.onSaveInstanceState returns AbsSavedState.EMPTY_STATE,
    and View.onRestoreInstanceState expects to be passed this. Also,
    both superclass methods MUST be called in my overrides (the docs
    don't make this clear).
*/

    protected static class SavedDrawViewState extends android.view.AbsSavedState
      {
        public static android.os.Parcelable.Creator<SavedDrawViewState> CREATOR =
            new android.os.Parcelable.Creator<SavedDrawViewState>()
              {
                public SavedDrawViewState createFromParcel
                  (
                    android.os.Parcel SavedState
                  )
                  {
                    System.err.println("DrawView.SavedDrawViewState.createFromParcel"); /* debug */
                    final android.view.AbsSavedState SuperState =
                        android.view.AbsSavedState.CREATOR.createFromParcel(SavedState);
                    final android.os.Bundle MyState = SavedState.readBundle();
                    return
                        new SavedDrawViewState
                          (
                            SuperState,
                            MyState.getFloat("ScrollX", 0.0f),
                            MyState.getFloat("ScrollY", 0.0f),
                            MyState.getFloat("ZoomFactor", 1.0f)
                          );
                  } /*createFromParcel*/

                public SavedDrawViewState[] newArray
                  (
                    int NrElts
                  )
                  {
                    return
                        new SavedDrawViewState[NrElts];
                  } /*newArray*/
              } /*Parcelable.Creator*/;

        public final android.os.Parcelable SuperState;
      /* state that I'm actually interested in saving/restoring: */
        public final float ScrollX, ScrollY, ZoomFactor;

        public SavedDrawViewState
          (
            android.os.Parcelable SuperState,
            float ScrollX,
            float ScrollY,
            float ZoomFactor
          )
          {
            super(SuperState);
            this.SuperState = SuperState;
            this.ScrollX = ScrollX;
            this.ScrollY = ScrollY;
            this.ZoomFactor = ZoomFactor;
          } /*SavedDrawViewState*/

        public void writeToParcel
          (
            android.os.Parcel SavedState,
            int Flags
          )
          {
            System.err.println("DrawView.SavedDrawViewState.writeToParcel"); /* debug */
            super.writeToParcel(SavedState, Flags);
          /* put my state in a Bundle, where each item is associated with a
            keyword name (unlike the Parcel itself, where items are identified
            by order). I think this makes things easier to understand. */
            final android.os.Bundle MyState = new android.os.Bundle();
            MyState.putFloat("ScrollX", ScrollX);
            MyState.putFloat("ScrollY", ScrollY);
            MyState.putFloat("ZoomFactor", ZoomFactor);
            SavedState.writeBundle(MyState);
          } /*writeToParcel*/

      } /*SavedDrawViewState*/

    @Override
    public android.os.Parcelable onSaveInstanceState()
      {
        System.err.println("DrawView called to save instance state"); /* debug */
        return
            new SavedDrawViewState
              (
                super.onSaveInstanceState(),
                ScrollOffset.x,
                ScrollOffset.y,
                ZoomFactor
              );
      } /*onSaveInstanceState*/

    @Override
    public void onRestoreInstanceState
      (
        android.os.Parcelable SavedState
      )
      {
        System.err.println("DrawView called to restore instance state " + (SavedState != null ? "non-null" : "null")); /* debug */
        final SavedDrawViewState MyState = (SavedDrawViewState)SavedState;
        super.onRestoreInstanceState(MyState.SuperState);
        ScrollOffset.x = MyState.ScrollX;
        ScrollOffset.y = MyState.ScrollY;
        ZoomFactor = MyState.ZoomFactor;
        invalidate();
      } /*onRestoreInstanceState*/

/*
    public widget-control methods
*/

    public void SetDrawer
      (
        Drawer DrawWhat
      )
      {
        this.DrawWhat = DrawWhat;
      /* fixme: need to rebuild cache and redraw if I allow user to change image on the fly */
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
            final PointF ViewCenter = new PointF(getWidth() / 2.0f, getHeight() / 2.0f);
            final PointF NewScrollOffset = /* so point of image at ViewCenter stays there */
                FindScrollOffset
                  (
                    /*DrawCoords =*/ ViewToDraw(ViewCenter),
                    /*ViewCoords =*/ ViewCenter,
                    /*ZoomFactor =*/ NewZoomFactor
                  );
            ZoomFactor = NewZoomFactor;
            ScrollTo(NewScrollOffset);
            invalidate();
          } /*if*/
      } /*ZoomBy*/

    public void ScrollTo
      (
        PointF NewScrollOffset
      )
      /* sets ScrollOffset to the specified value. */
      {
        if (DrawWhat != null)
          {
            NewScrollOffset = new PointF
              (
                Math.max(0.0f, Math.min(NewScrollOffset.x, 1.0f)),
                Math.max(0.0f, Math.min(NewScrollOffset.y, 1.0f))
              );
            if (ScrollOffset.x != NewScrollOffset.x || ScrollOffset.y != NewScrollOffset.y)
              {
                ScrollOffset = NewScrollOffset;
                invalidate();
              } /*if*/
          } /*if*/
      } /*ScrollTo*/

/*
    Implementing the following (and calling setxxxFadingEdgeEnabled(true)
    in the constructors, above) will cause fading edges to appear.
*/

    protected static final int ScrollScale = 1000; /* arbitrary units */

    @Override
    protected int computeHorizontalScrollExtent()
      {
        final RectF ViewBounds = GetScrolledViewBounds();
        return
            (int)Math.round(getWidth() * ScrollScale / (ViewBounds.right - ViewBounds.left));
      } /*computeHorizontalScrollExtent*/

    @Override
    protected int computeHorizontalScrollOffset()
      {
        final RectF ViewBounds = GetScrolledViewBounds();
        return
            ViewBounds.right - ViewBounds.left > getWidth() ?
                (int)Math.round
                  (
                        (- ViewBounds.left)
                    *
                        ScrollScale
                    /
                        (ViewBounds.right - ViewBounds.left - getWidth())
                  )
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
        final RectF ViewBounds = GetScrolledViewBounds();
        return
            (int)Math.round(getHeight() * ScrollScale / (ViewBounds.bottom - ViewBounds.top));
      } /*computeVerticalScrollExtent*/

    @Override
    protected int computeVerticalScrollOffset()
      {
        final RectF ViewBounds = GetScrolledViewBounds();
        return
            ViewBounds.bottom - ViewBounds.top > getHeight() ?
                (int)Math.round
                  (
                        (- ViewBounds.top)
                    *
                        ScrollScale
                    /
                        (ViewBounds.bottom - ViewBounds.top - getHeight())
                  )
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
