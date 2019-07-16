package cn.npe1348.lib_lockpatternview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义九宫格
 * 1.绘制九个格子,每排三个,每个格子占屏幕宽的1/3(注意,竖屏就是占高的1/3)
 */
public class LockPatternView extends View {
    private Context mContext;
    private String mPassword = "123456";
    private Point[][] mPoints = new Point[3][3];
    // 外圆的半径
    private int mOuterDotRadius = 80;
    //内圆半径
    private int mInnerDotRadius = 15;
    //外圆环宽度
    private int mOuterDotStroke = 5;
    //线宽度
    private int mLineStroke = 5;

    private int mNormalColor = Color.BLACK;
    private int mPressColor = Color.BLUE;
    private int mPassColor = Color.GREEN;
    private int mErrorColor = Color.RED;

    private boolean mIsInit = false;

    private boolean mIsTouchPoint = false;

    // 默认的外圆画笔
    private Paint mOuterNormalPaint;
    // 默认的内圆画笔
    private Paint mInnerNormalPaint;

    // 按下的外圆画笔
    private Paint mOuterPressedPaint;
    // 按下的内圆画笔
    private Paint mInnerPressedPaint;
    //默认线画笔
    private Paint mLinePaint;

    private Paint mErrorLinePaint;
    private Paint mPassLinePaint;
    private Paint mOuterErrorPaint;
    private Paint mOuterPassPaint;
    private Paint mInnerErrorPaint;
    private Paint mInnerPassPaint;

    private List<Point> mSelectPoints = new ArrayList<Point>();

    private float moveX;
    private float moveY;

    private OnUnLockListener mOnUnLockListener;

    public void setUnLockListener(OnUnLockListener onUnLockListener){
        this.mOnUnLockListener = onUnLockListener;
    }

    public interface OnUnLockListener{
        public void isUnLockSuccess(boolean success,String selectIndexStr);
    }

    public int dp2px(int dpValue) {
        return (int) (0.5f + dpValue * mContext.getResources().getDisplayMetrics().density);
    }

    public LockPatternView(Context context) {
        this(context,null);
    }

    public LockPatternView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public LockPatternView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        TypedArray array = context.obtainStyledAttributes(attrs,R.styleable.LockPatternView);
        mOuterDotRadius = array.getDimensionPixelSize(R.styleable.LockPatternView_outer_dot_radius,dp2px(mOuterDotRadius));
        mInnerDotRadius = array.getDimensionPixelSize(R.styleable.LockPatternView_inner_dot_radius,dp2px(mInnerDotRadius));
        mOuterDotStroke = array.getDimensionPixelSize(R.styleable.LockPatternView_outer_dot_stroke,dp2px(mOuterDotStroke));
        mLineStroke = array.getDimensionPixelSize(R.styleable.LockPatternView_line_stroke,dp2px(mLineStroke));

        mNormalColor = array.getColor(R.styleable.LockPatternView_normal_color,mNormalColor);
        mPressColor = array.getColor(R.styleable.LockPatternView_press_color,mPressColor);
        mPassColor = array.getColor(R.styleable.LockPatternView_pass_color,mPassColor);
        mErrorColor = array.getColor(R.styleable.LockPatternView_error_color,mErrorColor);
        array.recycle();
    }

    public void setPassword(String password) {
        this.mPassword = mPassword;
    }

    private void initDot(){
        int mWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int mHeight = getHeight() - getPaddingTop() - getPaddingBottom();


        int offsetX = 0;
        int offsetY = 0;

        //这里就是我们支持横屏或者竖屏的代码
        if (mWidth > mHeight) {
            offsetX = (mWidth - mHeight) / 2 + getPaddingLeft();
            offsetY = getPaddingTop();
            mWidth = mHeight;
        } else {
            offsetX = getPaddingLeft();
            offsetY = (mHeight - mWidth) / 2 + getPaddingTop();
            mHeight = mWidth;
        }
        //每个点占1/3
        int squareWidth = mWidth/3;

        for (int i = 0;i<mPoints.length;i++) {
            for (int j = 0;j<mPoints[i].length;j++) {
                // 循环获取九个点
                mPoints[i][j] = new Point(offsetX + squareWidth * (j * 2 + 1) / 2,
                        offsetY + squareWidth * (i * 2 + 1) / 2, i * mPoints.length + j+1);
            }
        }
    }

    /**
     * 初始化画笔
     *
     */
    private void initPaint() {
        // 默认的外圆画笔
        mOuterNormalPaint = new Paint();
        //颜色可以自定义
        mOuterNormalPaint.setColor(mNormalColor);
        //外圆为空心的
        mOuterNormalPaint.setStyle(Paint.Style.STROKE);
        mOuterNormalPaint.setAntiAlias(true);
        mOuterNormalPaint.setStrokeWidth(mOuterDotStroke);


        // 默认的内圆画笔
        mInnerNormalPaint = new Paint();
        //颜色可以自定义
        mInnerNormalPaint.setColor(mNormalColor);
        //内圆为实心的
        mInnerNormalPaint.setStyle(Paint.Style.FILL);
        mInnerNormalPaint.setAntiAlias(true);

        // 按下的外圆画笔
        mOuterPressedPaint = new Paint();
        mOuterPressedPaint.setColor(mPressColor);
        mOuterPressedPaint.setStyle(Paint.Style.STROKE);
        mOuterPressedPaint.setAntiAlias(true);
        mOuterPressedPaint.setStrokeWidth(mOuterDotStroke);

        // 按下的内圆画笔
        mInnerPressedPaint = new Paint();
        mInnerPressedPaint.setColor(mPressColor);
        mInnerPressedPaint.setStyle(Paint.Style.FILL);
        mInnerPressedPaint.setAntiAlias(true);

        // 线的画笔
        mLinePaint = new Paint();
        mLinePaint.setColor(mPressColor);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStrokeWidth(mLineStroke);

        // 错误的线的画笔
        mErrorLinePaint = new Paint();
        mErrorLinePaint.setColor(mErrorColor);
        mErrorLinePaint.setStyle(Paint.Style.STROKE);
        mErrorLinePaint.setAntiAlias(true);
        mErrorLinePaint.setStrokeWidth(mLineStroke);
        // 通过的线的画笔
        mPassLinePaint = new Paint();
        mPassLinePaint.setColor(mPassColor);
        mPassLinePaint.setStyle(Paint.Style.STROKE);
        mPassLinePaint.setAntiAlias(true);
        mPassLinePaint.setStrokeWidth(mLineStroke);

        //错误的外圆画笔
        mOuterErrorPaint = new Paint();
        mOuterErrorPaint.setColor(mErrorColor);
        mOuterErrorPaint.setStyle(Paint.Style.STROKE);
        mOuterErrorPaint.setAntiAlias(true);
        mOuterErrorPaint.setStrokeWidth(mOuterDotStroke);
        //通过的外圆画笔
        mOuterPassPaint = new Paint();
        mOuterPassPaint.setColor(mPassColor);
        mOuterPassPaint.setStyle(Paint.Style.STROKE);
        mOuterPassPaint.setAntiAlias(true);
        mOuterPassPaint.setStrokeWidth(mOuterDotStroke);

        //错误的内圆画笔
        mInnerErrorPaint = new Paint();
        mInnerErrorPaint.setColor(mErrorColor);
        mInnerErrorPaint.setStyle(Paint.Style.FILL);
        mInnerErrorPaint.setAntiAlias(true);
        //通过的内圆画笔
        mInnerPassPaint = new Paint();
        mInnerPassPaint.setColor(mPassColor);
        mInnerPassPaint.setStyle(Paint.Style.FILL);
        mInnerPassPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!mIsInit){
            initDot();
            initPaint();
            mIsInit = true;
        }
        drawDot(canvas);
        if (mSelectPoints.size()>0){
            drawLine(canvas);
        }
    }

    private void drawLine(Canvas canvas){
        //绘制最后一点到当前位置的线
        if (moveX != 0 && moveY != 0) {
            Point lastPoint = mSelectPoints.get(mSelectPoints.size() - 1);
            canvas.drawLine(lastPoint.centerX, lastPoint.centerY, moveX, moveY, mLinePaint);
        }
        if (mSelectPoints.size()>1) {
            for (int i = 0; i < mSelectPoints.size()-1; i++) {
                Point point = mSelectPoints.get(i);
                Point nextPoint = mSelectPoints.get(i+1);
                if(Point.STATUS_PRESSED == point.status){
                    canvas.drawLine(point.centerX, point.centerY, nextPoint.centerX, nextPoint.centerY, mLinePaint);
                }else if(Point.STATUS_ERROR == point.status){
                    canvas.drawLine(point.centerX, point.centerY, nextPoint.centerX, nextPoint.centerY, mErrorLinePaint);
                }else if(Point.STATUS_PASS == point.status){
                    canvas.drawLine(point.centerX, point.centerY, nextPoint.centerX, nextPoint.centerY, mPassLinePaint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mIsTouchPoint = false;
                Point pressPoint = getTouchPoint(event.getX(),event.getY());
                if (null != pressPoint){
                    mSelectPoints.add(pressPoint);
                    pressPoint.status = Point.STATUS_PRESSED;
                    mIsTouchPoint = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsTouchPoint) {
                    moveX = event.getX();
                    moveY = event.getY();
                    Point movePoint = getTouchPoint(moveX, moveY);
                    if (null != movePoint) {
                        if (!mSelectPoints.contains(movePoint)) {
                            mSelectPoints.add(movePoint);
                        }
                        movePoint.status = Point.STATUS_PRESSED;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsTouchPoint) {
                    moveX = 0;
                    moveY = 0;
                    String selectIndexStr = getSelectPointIndexStr();
                    if (null != mPassword && mPassword.equals(selectIndexStr)) {
                        if (null != mOnUnLockListener) {
                            mOnUnLockListener.isUnLockSuccess(true,selectIndexStr);
                        }
                        setAllSelectPointPass();
                    }else{
                        if (null != mOnUnLockListener) {
                            mOnUnLockListener.isUnLockSuccess(false,selectIndexStr);
                        }
                        setAllSelectPointError();
                    }
                }
                break;
        }
        invalidate();
        return true;
    }
    /**
     * 获取选中的下标字符串
     * @return
     */
    private String getSelectPointIndexStr(){
        String indexStr = "";
        for (int i = 0; i < mSelectPoints.size(); i++){
            indexStr+=mSelectPoints.get(i).index;
        }
        return indexStr;
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            clearSelectPointStatus();
        }
    };

    /**
     * 设置错误状态
     */
    private void setAllSelectPointError(){
        for (int i = 0; i < mSelectPoints.size(); i++){
            mSelectPoints.get(i).status = Point.STATUS_ERROR;
        }
        invalidate();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(Point.STATUS_ERROR);
            }
        },1000);
    }

    /**
     * 设置通过状态
     */
    private void setAllSelectPointPass(){
        for (int i = 0; i < mSelectPoints.size(); i++){
            mSelectPoints.get(i).status = Point.STATUS_PASS;
        }
        invalidate();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(Point.STATUS_PASS);
            }
        },1000);
    }

    /**
     * 清除选中点的状态
     */
    private void clearSelectPointStatus(){
        for (int i = 0;i<mPoints.length;i++) {
            for (int j = 0;j<mPoints[i].length;j++) {
                // 循环绘制九个点
                mPoints[i][j].status = Point.STATUS_NORMAL;
            }
        }
        mSelectPoints.clear();
        invalidate();
    }


    //获取当前触摸位置的点
    private Point getTouchPoint(float x,float y){
        for (int i = 0;i<mPoints.length;i++) {
            for (int j = 0;j<mPoints[i].length;j++) {
                // 循环遍历九个点
                Point point = mPoints[i][j];

                if (x>point.centerX-mOuterDotRadius && x < point.centerX+mOuterDotRadius && y>point.centerY-mOuterDotRadius && y<point.centerY+mOuterDotRadius){
                    return point;
                }
            }
        }
        return null;
    }

    private void drawDot(Canvas canvas) {
        for (int i = 0;i<mPoints.length;i++) {
            for (int j = 0;j<mPoints[i].length;j++) {
                // 循环绘制九个点
                Point point = mPoints[i][j];
                if (Point.STATUS_NORMAL == point.status) {
                    canvas.drawCircle(point.centerX, point.centerY, mInnerDotRadius, mInnerNormalPaint);
                    canvas.drawCircle(point.centerX, point.centerY, mOuterDotRadius, mOuterNormalPaint);
                }else if(Point.STATUS_PRESSED == point.status){
                    canvas.drawCircle(point.centerX, point.centerY, mInnerDotRadius, mInnerPressedPaint);
                    canvas.drawCircle(point.centerX, point.centerY, mOuterDotRadius, mOuterPressedPaint);
                }else if(Point.STATUS_ERROR == point.status){
                    canvas.drawCircle(point.centerX, point.centerY, mInnerDotRadius, mInnerErrorPaint);
                    canvas.drawCircle(point.centerX, point.centerY, mOuterDotRadius, mOuterErrorPaint);
                }else if(Point.STATUS_PASS == point.status){
                    canvas.drawCircle(point.centerX, point.centerY, mInnerDotRadius, mInnerPassPaint);
                    canvas.drawCircle(point.centerX, point.centerY, mOuterDotRadius, mOuterPassPaint);
                }
            }
        }
    }

    class Point{
        //默认状态
        public static final int STATUS_NORMAL = 1;
        //选择状态
        public static final int STATUS_PRESSED = 2;
        //错误状态
        public static final int STATUS_ERROR = 3;
        //通过状态
        public static final int STATUS_PASS = 4;
        //中心点x
        int centerX;
        //中心点y
        int centerY;
        //下标,就是我们定义的密码1-9
        int index;
        //当前点的状态
        int status = STATUS_NORMAL;

        public Point(int centerX, int centerY, int index) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.index = index;
        }
    }
}
