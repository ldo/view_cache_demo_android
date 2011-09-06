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
    protected float ScrollX, ScrollY;
      /* range is [0.0 .. 1.0], such that 0 corresponds to left/top edge
        of image being at left/top edge of view, 0.5 corresponds to middle
        of image being in middle of view, and 1 corresponds to right/bottom
        edge of image being at right/bottom edge of view */
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

/*
    Mapping between image coordinates and view coordinates
*/

    protected class ViewParms
      /* parameters for scaling and positioning image display */
      {
        public final float DrawWidth, DrawHeight;
        public final float ViewWidth, ViewHeight;
        public final float ScaledViewWidth, ScaledViewHeight;
        public final float HorScrollLimit, VertScrollLimit;
        public final boolean CanScrollHoriz, CanScrollVert;

        public ViewParms
          (
            float ZoomFactor
          )
          {
            DrawWidth = DrawWhat.Bounds.right - DrawWhat.Bounds.left;
            DrawHeight = DrawWhat.Bounds.bottom - DrawWhat.Bounds.top;
            ViewWidth = getWidth();
            ViewHeight = getHeight();
            final float ScaledViewWidth = ViewWidth * ZoomFactor;
            final float ScaledViewHeight = ViewHeight * ZoomFactor;
            if (ScaledViewWidth > ScaledViewHeight * DrawWidth / DrawHeight)
              {
                this.ScaledViewWidth = ScaledViewHeight * DrawWidth / DrawHeight;
                this.ScaledViewHeight = ScaledViewHeight;
              }
            else if (ScaledViewHeight > ScaledViewWidth * DrawHeight / DrawWidth)
              {
                this.ScaledViewWidth = ScaledViewWidth;
                this.ScaledViewHeight = ScaledViewWidth * DrawHeight / DrawWidth;
              }
            else
              {
                this.ScaledViewWidth = ScaledViewWidth;
                this.ScaledViewHeight = ScaledViewHeight;
              } /*if*/
            HorScrollLimit = this.ScaledViewWidth - ViewWidth;
            VertScrollLimit = this.ScaledViewHeight - ViewHeight;
            CanScrollHoriz = HorScrollLimit > 0;
            CanScrollVert = VertScrollLimit > 0;
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
                        - v.HorScrollLimit
                    *
                        (v.CanScrollHoriz ? ScrollX : 0.5f),
                /*y =*/
                        - v.VertScrollLimit
                    *
                        (v.CanScrollVert ? ScrollY : 0.5f)
              );
      } /*ScrollOffset*/

/*
    View cache management & drawing
*/

    protected static class ViewCacheBits
      /* state of the view cache */
      {
        public final Bitmap Bits; /* cached part of image */
        public final RectF Bounds;
          /* such that (0, 0) maps to (DrawWhat.Bounds.left, DrawWhat.Bounds.top)
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
            final ViewParms v = new ViewParms();
            final PointF ViewOffset = ScrollOffset(v);
            final RectF DestRect = new RectF(ViewCache.Bounds);
            DestRect.offset(ViewOffset.x, ViewOffset.y);
            if
                (
                    DestRect.left > 0
                ||
                    DestRect.top > 0
                ||
                    DestRect.right <= v.ViewWidth
                ||
                    DestRect.bottom <= v.ViewHeight
                )
              {
              /* cache doesn't completely cover view */
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
                final RectF DestRect = new RectF(0, 0, v.ScaledViewWidth, v.ScaledViewHeight);
                DestRect.offset(ViewOffset.x, ViewOffset.y);
                DrawWhat.Draw(g, DestRect);
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
                    StartScroll.x + (EndScroll.x - StartScroll.x) * AnimAmt,
                    StartScroll.y + (EndScroll.y - StartScroll.y) * AnimAmt
                  );
                if (CurrentTime < EndTime)
                  {
                    getHandler().post(this);
                  }
                else
                  {
                    CurrentAnim = null;
                  } /*if*/
              } /*if*/
          } /*run*/

      } /*ScrollAnimator*/

    private ScrollAnimator CurrentAnim = null;

    protected android.view.GestureDetector FlingDetector =
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
                    final ViewParms v = new ViewParms();
                    final PointF ViewOffset = ScrollOffset(v);
                    final boolean DoFling =
                            v.CanScrollHoriz && XVelocity != 0
                        ||
                            v.CanScrollVert && YVelocity != 0;
                    if (DoFling)
                      {
                        final double CurrentTime = System.currentTimeMillis() / 1000.0;
                        final float ScrollDuration =
                                (float)Math.hypot(XVelocity, YVelocity)
                            /
                                (float)Math.hypot(v.ViewWidth, v.ViewHeight);
                        final float Attenuate = 2.0f;
                        final PointF StartScroll = /* current centre point in image */
                            new PointF
                              (
                                    DrawWhat.Bounds.left
                                +
                                        (v.ViewWidth / 2.0f - ViewOffset.x)
                                    /
                                        v.ScaledViewWidth
                                    *
                                        v.DrawWidth,
                                    DrawWhat.Bounds.top
                                +
                                        (v.ViewHeight / 2.0f - ViewOffset.y)
                                    /
                                        v.ScaledViewHeight
                                    *
                                        v.DrawHeight
                              );
                        final PointF EndScroll =
                            new PointF
                              (
                                    StartScroll.x
                                -
                                        XVelocity
                                    /
                                        v.ScaledViewWidth
                                    *
                                        v.DrawWidth
                                    *
                                        ScrollDuration
                                    /
                                        Attenuate,
                                    StartScroll.y
                                -
                                        YVelocity
                                    /
                                        v.ScaledViewHeight
                                    *
                                        v.DrawWidth
                                    *
                                        ScrollDuration
                                    /
                                        Attenuate
                              );
                        EndScroll.x =
                            Math.max
                              (
                                DrawWhat.Bounds.left,
                                Math.min(EndScroll.x, DrawWhat.Bounds.right)
                              );
                        EndScroll.y =
                            Math.max
                              (
                                DrawWhat.Bounds.top,
                                Math.min(EndScroll.y, DrawWhat.Bounds.bottom)
                              );
                        new ScrollAnimator
                          (
                            /*AnimFunction =*/ new android.view.animation.DecelerateInterpolator(),
                            /*StartTime =*/ CurrentTime,
                            /*EndTime =*/ CurrentTime + ScrollDuration,
                            /*StartScroll =*/ StartScroll,
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
                            final ViewParms v = new ViewParms();
                            if (v.CanScrollHoriz && ThisMouse.x != LastMouse.x)
                              {
                                final float ScrollDelta =
                                    (ThisMouse.x - LastMouse.x) / - v.HorScrollLimit;
                                ScrollX = Math.max(0.0f, Math.min(1.0f, ScrollX + ScrollDelta));
                                super.invalidate();
                              } /*if*/
                            if (v.CanScrollVert && ThisMouse.y != LastMouse.y)
                              {
                                final float ScrollDelta =
                                    (ThisMouse.y - LastMouse.y) / - v.VertScrollLimit;
                                ScrollY = Math.max(0.0f, Math.min(1.0f, ScrollY + ScrollDelta));
                                super.invalidate();
                              } /*if*/
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
                    final ViewParms v = new ViewParms();
                    final PointF ViewOffset = ScrollOffset(v);
                    final PointF NewCenter = new PointF
                      (
                        (DrawWhat.Bounds.right + DrawWhat.Bounds.left) / 2.0f,
                        (DrawWhat.Bounds.bottom + DrawWhat.Bounds.top) / 2.0f
                      );
                    final PointF LastMouse =
                        LastMouse2 != null ?
                            new PointF
                              (
                                (LastMouse1.x + LastMouse2.x) / 2.0f,
                                (LastMouse1.y + LastMouse2.y) / 2.0f
                              )
                        :
                            LastMouse1;
                    if (v.CanScrollHoriz)
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
                    if (v.CanScrollVert)
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
                            MyState.getFloat("ScrollX", 0.5f),
                            MyState.getFloat("ScrollY", 0.5f),
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
                ScrollX,
                ScrollY,
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
        ScrollX = MyState.ScrollX;
        ScrollY = MyState.ScrollY;
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
          /* try to adjust scroll offset so point in image at centre of view stays in centre */
            final ViewParms v1 = new ViewParms();
            final ViewParms v2 = new ViewParms(NewZoomFactor);
            if (v1.CanScrollHoriz && v2.CanScrollHoriz)
              {
                ScrollX =
                        (
                                (
                                    v1.ViewWidth / 2.0f
                                +
                                    ScrollX * v1.HorScrollLimit
                                )
                            /
                                v1.ScaledViewWidth
                            *
                                v2.ScaledViewWidth
                        -
                            v2.ViewWidth / 2.0f
                        )
                    /
                        v2.HorScrollLimit;
              } /*if*/
            if (v1.CanScrollVert && v2.CanScrollVert)
              {
                ScrollY =
                        (
                                (
                                    v1.ViewHeight / 2.0f
                                +
                                    ScrollY * v1.VertScrollLimit
                                )
                            /
                                v1.ScaledViewHeight
                            *
                                v2.ScaledViewHeight
                        -
                            v2.ViewHeight / 2.0f
                        )
                    /
                        v2.VertScrollLimit;
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
      /* tries to ensure the specified position (in image coordinates)
        is at the centre of the view. */
      {
        if (DrawWhat != null)
          {
            final ViewParms v = new ViewParms();
            final float OldScrollX = ScrollX;
            final float OldScrollY = ScrollY;
            if (v.CanScrollHoriz)
              {
                ScrollX =
                        (
                            (X - DrawWhat.Bounds.left) / v.DrawWidth * v.ScaledViewWidth
                        -
                            v.ViewWidth / 2.0f
                        )
                    /
                        v.HorScrollLimit;
                ScrollX = Math.max(0.0f, Math.min(1.0f, ScrollX));
              } /*if*/
            if (v.CanScrollVert)
              {
                ScrollY =
                        (
                            (Y - DrawWhat.Bounds.top) / v.DrawHeight * v.ScaledViewHeight
                        -
                            v.ViewHeight / 2.0f
                        )
                    /
                        v.VertScrollLimit;
                ScrollY = Math.max(0.0f, Math.min(1.0f, ScrollY));
              } /*if*/
            if (OldScrollX != ScrollX || OldScrollY != ScrollY)
              {
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
        final ViewParms v = new ViewParms();
        return
            (int)Math.round(v.ViewWidth * ScrollScale / v.ScaledViewWidth);
      } /*computeHorizontalScrollExtent*/

    @Override
    protected int computeHorizontalScrollOffset()
      {
        final ViewParms v = new ViewParms();
        return
            v.CanScrollHoriz ?
                (int)Math.round
                  (
                    ScrollX * ScrollScale * v.HorScrollLimit / v.ScaledViewWidth
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
        final ViewParms v = new ViewParms();
        return
            (int)Math.round(v.ViewHeight * ScrollScale / v.ScaledViewHeight);
      } /*computeVerticalScrollExtent*/

    @Override
    protected int computeVerticalScrollOffset()
      {
        final ViewParms v = new ViewParms();
        return
            v.CanScrollVert ?
                (int)Math.round
                  (
                    ScrollY * ScrollScale * v.VertScrollLimit / v.ScaledViewHeight
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
