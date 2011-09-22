package nz.gen.geek_central.view_cache_demo;
/*
    Interface for class rendering image to be displayed by DrawView.

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

public interface Drawer
  {

    public android.graphics.RectF GetBounds();
      /* returns the bounds of the image in its own coordinate system. */

    public void Draw
      (
        android.graphics.Canvas Dest,
        android.graphics.RectF DestRect
      );
      /* draws the image scaled to fit the specified destination rectangle. */

  } /*Drawer*/
