package nz.gen.geek_central.view_cache_demo;
/*
    Sample class for drawing an image that takes an appreciable
    fraction of a second to render.

    Written by Lawrence D'Oliveiro <ldo@geek-central.gen.nz>.
*/

import android.graphics.PointF;
import android.graphics.RectF;

public class Drawer
  {
    public final RectF Bounds; /* bounds of image in user space */
    protected final int StrokeColor = 0xff240896;
    protected final int BackgroundColor = 0xffbdaa7d;
    protected final float Radius = 250.0f;
    protected final int NrLevels = 12;
    protected final float Kink = 0.8f;

    public Drawer()
      {
        Bounds = new RectF(-Radius, -Radius, Radius, Radius);
      } /*Drawer*/

    protected void DrawSegment
      (
        android.graphics.Path In,
        PointF From,
        PointF To,
        int Level
      )
      {
        if (Level > 0)
          {
            final PointF Mid = new PointF
              (
                (From.x + To.x) / 2.0f,
                (From.y + To.y) / 2.0f
              );
            final PointF Delta = new PointF(To.x - From.x, To.y - From.y);
            final PointF Corner = new PointF
              (
                Mid.x + Delta.y * 0.5f * Kink,
                Mid.y - Delta.x * 0.5f * Kink
              );
              /* so Corner forms right angle between From and To points */
          /* note reversed order of points in recursive calls so subdivided corners go opposite way */
            DrawSegment(In, To, Corner, Level - 1);
            DrawSegment(In, Corner, From, Level - 1);
          }
        else
          {
            In.moveTo(From.x, From.y);
            In.lineTo(To.x, To.y);
          } /*if*/
      } /*DrawSegment*/

    public void Draw
      (
        android.graphics.Canvas Dest,
        RectF DestRect
      )
      {
        final android.graphics.Matrix ViewMapping = new android.graphics.Matrix();
        ViewMapping.setRectToRect
          (
            /*src =*/ Bounds,
            /*dst =*/ DestRect,
            /*stf =*/ android.graphics.Matrix.ScaleToFit.CENTER
          );
          {
            final android.graphics.Paint BG = new android.graphics.Paint();
            BG.setStyle(android.graphics.Paint.Style.FILL);
            BG.setColor(BackgroundColor);
            Dest.drawPaint(BG);
          }
          {
            final android.graphics.Path p = new android.graphics.Path();
            DrawSegment
              (
                /*In =*/ p,
                /*From =*/ new PointF(-Radius, Radius),
                /*To =*/ new PointF(Radius, Radius),
                /*Level =*/ NrLevels
              );
            DrawSegment
              (
                /*In =*/ p,
                /*From =*/ new PointF(Radius, Radius),
                /*To =*/ new PointF(Radius, -Radius),
                /*Level =*/ NrLevels
              );
            DrawSegment
              (
                /*In =*/ p,
                /*From =*/ new PointF(Radius, -Radius),
                /*To =*/ new PointF(-Radius, -Radius),
                /*Level =*/ NrLevels
              );
            DrawSegment
              (
                /*In =*/ p,
                /*From =*/ new PointF(-Radius, -Radius),
                /*To =*/ new PointF(-Radius, Radius),
                /*Level =*/ NrLevels
              );
            p.close();
            p.transform(ViewMapping);
            final android.graphics.Paint How = new android.graphics.Paint();
            How.setAntiAlias(true);
            How.setStyle(android.graphics.Paint.Style.STROKE);
            How.setColor(StrokeColor);
            How.setStrokeWidth(2.0f); /* note this is not scaled by ViewMapping */
            Dest.drawPath(p, How);
          }
      } /*Draw*/

  } /*Drawer*/
