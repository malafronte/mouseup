package demo.project.hmi.demoapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.ViewGroup;

/**
 * Created by Matteo on 26/04/2016.
 */

class OverlayView extends ViewGroup {
    private Paint mLoadPaint;
    boolean mShowCursor;
    Bitmap cursor;
    public int x = 0,y = 1000;
    public void Update(int nx, int ny) {
        x = nx;
        y = ny;
    }
    public void ShowCursor(boolean status) {
        mShowCursor = status;
    }
    public boolean isCursorShown() {
        return mShowCursor;
    }
    public void setCursor(){
        cursor = BitmapFactory.decodeResource(getResources(), R.drawable.mouse);
    }
    public OverlayView(Context context) {
        super(context);
        cursor = BitmapFactory.decodeResource(context.getResources(), R.drawable.mouse);

        mLoadPaint = new Paint();
        mLoadPaint.setAntiAlias(true);
        mLoadPaint.setTextSize(10);
        mLoadPaint.setARGB(255, 255, 0, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mShowCursor) canvas.drawBitmap(cursor,x,y,null);
    }
    @Override
    protected void onLayout(boolean arg0, int arg1, int arg2, int arg3, int arg4) {}
}
