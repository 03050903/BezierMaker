/*
 * Copyright (C) 2016 venshine.cn@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wx.beziermaker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * @author venshine
 */
public class Bezier extends View {

    public static final int COUNT = 9;  // 贝塞尔曲线阶数
    public static final int REGION_WIDTH = 30;  // 合法区域宽度
    public static final int FINGER_RECT_SIZE = 60;   // 矩形尺寸
    public static final int BEZIER_WIDTH = 10;   // 贝塞尔曲线线宽
    public static final int TANGENT_WIDTH = 10;  // 切线线宽
    public static final int CONTROL_WIDTH = 12;    // 控制点连线线宽
    public static final int CONTROL_RADIUS = 12;  // 控制点半径

    private Path mBezierPath = null;    // 贝塞尔曲线路径

    private Paint mBezierPaint = null;  // 贝塞尔曲线画笔
    private Paint mMovingPaint = null;  // 移动点画笔
    private Paint mControlPaint = null;  // 控制点画笔
    private Paint mTangentPaint = null;  // 切线画笔
    private Paint mLinePaint = null;    // 固定线画笔
    private Paint mTextPaint = null;    // 文字画笔

    private ArrayList<PointF> mBezierPoints = null; // 贝塞尔曲线点集
    private PointF mBezierPoint = null; // 贝塞尔曲线移动点

    private ArrayList<PointF> mControlPoints;    // 控制点集

    private ArrayList<PointF> mPoints1 = new ArrayList<>();
    private ArrayList<PointF> mPoints2 = new ArrayList<>();

    private int i = 0;

    private int mCurOrder = 2;  // 当前阶数

    private boolean mRun = true;   // 运行状态

    private boolean mTouch = true; // 控制状态

    private int mWidth = 0, mHeight = 0;    // 画布宽高

    private PointF mCurPoint; // 当前移动的控制点

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                i += 10;
//                log("message:" + i + ", size:" + mBezierPoints.size());
                if (i >= mBezierPoints.size()/* || i >= mPoints1.size() || i >= mPoints2.size()*/) {
                    removeMessages(100);
                    i = 0;
                    mRun = false;
                    mTouch = true;
                    return;
                }
                mBezierPoint = new PointF(mBezierPoints.get(i).x, mBezierPoints.get(i).y);
//                log("message point:" + mBezierPoint);
//                mP1 = new PointF(mPoints1.get(i).x, mPoints1.get(i).y);
//                mP2 = new PointF(mPoints2.get(i).x, mPoints2.get(i).y);
                invalidate();
            }
        }
    };

    public Bezier(Context context) {
        super(context);
        init();
    }

    public Bezier(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Bezier(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 初始坐标
        mControlPoints = new ArrayList<>(COUNT + 1);
        mControlPoints.add(new PointF(100, 100));
        mControlPoints.add(new PointF(200, 300));
        mControlPoints.add(new PointF(500, 200));
//        mControlPoints.add(new PointF(300, 200));
//        mControlPoints.add(new PointF(400, 300));

        // 贝塞尔曲线画笔
        mBezierPaint = new Paint();
        mBezierPaint.setColor(Color.RED);
        mBezierPaint.setStrokeWidth(BEZIER_WIDTH);
        mBezierPaint.setStyle(Paint.Style.STROKE);
        mBezierPaint.setAntiAlias(true);

        // 移动点画笔
        mMovingPaint = new Paint();
        mMovingPaint.setColor(Color.BLACK);
        mMovingPaint.setAntiAlias(true);
        mMovingPaint.setStyle(Paint.Style.FILL);

        // 控制点画笔
        mControlPaint = new Paint();
        mControlPaint.setColor(Color.BLACK);
        mControlPaint.setAntiAlias(true);
        mControlPaint.setStyle(Paint.Style.STROKE);

        // 切线画笔
        mTangentPaint = new Paint();
        mTangentPaint.setColor(Color.GREEN);
        mTangentPaint.setAntiAlias(true);
        mTangentPaint.setStrokeWidth(TANGENT_WIDTH);
        mTangentPaint.setStyle(Paint.Style.FILL);

        // 固定线画笔
        mLinePaint = new Paint();
        mLinePaint.setColor(Color.LTGRAY);
        mLinePaint.setStrokeWidth(CONTROL_WIDTH);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setStyle(Paint.Style.FILL);

        // 文字画笔
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(45);

        mBezierPath = new Path();
    }

    private ArrayList<PointF> initBezierPoints(ArrayList<PointF> controlPoints) {
        ArrayList<PointF> points = new ArrayList<>();
        for (float t = 0; t <= 1; t += 0.001f) {
//            PointF pointF = createBezier(controlPoints, t);
//            PointF pointF = deCasteljau(controlPoints, t);
            PointF pointF = bezier(controlPoints, t);
            points.add(pointF);
        }
        return points;
    }

    public PointF bezier(ArrayList<PointF> controlPoints, float t) {
        int order = controlPoints.size() - 1;
        float x, y;
        switch (order) {
            case 1:
                x = (1 - t) * controlPoints.get(0).x + t * controlPoints.get(1).x;
                y = (1 - t) * controlPoints.get(0).y + t * controlPoints.get(1).y;
                return new PointF(x, y);
            case 2:
                x = (float) (Math.pow((1 - t), 2) * controlPoints.get(0).x + 2 * t * (1 - t) * controlPoints.get(1).x
                        + Math.pow(t, 2) *
                        controlPoints.get(2).x);
                y = (float) (Math.pow((1 - t), 2) * controlPoints.get(0).y + 2 * t * (1 - t) * controlPoints.get(1).y
                        + Math.pow(t, 2) *
                        controlPoints.get(2).y);
                return new PointF(x, y);
            case 3:
                x = (float) (Math.pow((1 - t), 3) * controlPoints.get(0).x + 3 * t * Math.pow((1 - t), 2) *
                        controlPoints.get(1).x + 3 * Math.pow(t, 2) * (1 - t) * controlPoints.get(2).x + Math.pow(t,
                        3) * controlPoints.get(3).x);
                y = (float) (Math.pow((1 - t), 3) * controlPoints.get(0).y + 3 * t * Math.pow((1 - t), 2) *
                        controlPoints.get(1).y + 3 * Math.pow(t, 2) * (1 - t) * controlPoints.get(2).y + Math.pow(t,
                        3) * controlPoints.get(3).y);
                return new PointF(x, y);
            case 4:
                x = (float) (Math.pow((1 - t), 4) * controlPoints.get(0).x + 4 * t * Math.pow((1 - t), 3) *
                        controlPoints.get
                                (1).x + 4 * Math.pow(t, 2) * Math.pow((1 - t), 2) * controlPoints.get(2).x + 4 * Math
                        .pow(t,
                                3) * (1 - t) * controlPoints.get(3).x + Math.pow(t, 4) * controlPoints.get(4).x);
                y = (float) (Math.pow((1 - t), 4) * controlPoints.get(0).y + 4 * t * Math.pow((1 - t), 3) *
                        controlPoints.get
                                (1).y + 4 * Math.pow(t, 2) * Math.pow((1 - t), 2) * controlPoints.get(2).y + 4 * Math
                        .pow(t,
                                3) * (1 - t) * controlPoints.get(3).y + Math.pow(t, 4) * controlPoints.get(4).y);
                return new PointF(x, y);
        }
        return null;
    }

    public static PointF deCasteljau(ArrayList<PointF> controlPoints, float t) {
        final int n = controlPoints.size();
        PointF[] points = new PointF[n];
        for (int i = 0; i < n; i++) {
            points[i] = controlPoints.get(i);
        }
        for (int i = 1; i <= n; i++)
            for (int j = 0; j < n - i; j++) {
                points[j].x = (1 - t) * points[j].x + t * points[j + 1].x;
                points[j].y = (1 - t) * points[j].y + t * points[j + 1].y;
            }

        return points[0];
    }

    private PointF createBezier(ArrayList<PointF> controlPoints, float t) {
        int size = controlPoints.size();
        PointF[] temps = new PointF[size];
        for (int i = 0; i < size; i++) {
            temps[i] = controlPoints.get(i);
        }
//        for (PointF pointF : temps) {
//            log("create:" + pointF);
//        }
        float temp = temps[0].x;
        float tempp = temps[0].x;
        float temp1 = temps[0].x;
        float temp2 = temps[0].x;
        float temp3 = temps[0].x;
        float temp4 = temps[0].x;

        for (int i = 1; i <= size; i++) {
            for (int j = 0; j < size - i; j++) {
                temp1 = temp2 * (1 - t) + temps[j + 1].x * t;
                temp2 = temp1;
                temp3 = temp4 * (1 - t) + temps[j + 1].y * t;
                temp4 = temp3;
            }
        }

        return new PointF(temp2, temp4);
    }

    private ArrayList<PointF> createQuad(PointF start, PointF con, PointF end) {
        ArrayList<PointF> points = new ArrayList<>();
        float x, y;
        for (float t = 0; t <= 1; t += 0.001f) {
            x = (1 - t) * (1 - t) * start.x + 2 * t * (1 - t) * con.x + t * t * end.x;
            y = (1 - t) * (1 - t) * start.y + 2 * t * (1 - t) * con.y + t * t * end.y;
            log("quad x:" + x + ", y:" + y);
            points.add(new PointF(x, y));
        }
//        for (float i = 0; i <= 1; i += 0.001f) {
//            float xx = start.x + (con.x - start.x) * i;
//            float yy = start.y + (con.y - start.y) * i;
//            float xxx = con.x + (end.x - con.x) * i;
//            float yyy = con.y + (end.y - con.y) * i;
//            mPoints1.add(new PointF(xx, yy));
//            mPoints2.add(new PointF(xxx, yyy));
//        }
        return points;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mWidth == 0 || mHeight == 0) {
            mWidth = getWidth();
            mHeight = getHeight();
        }
    }

    /**
     * 判断坐标是否在合法区域中
     *
     * @param x
     * @param y
     * @return
     */
    private boolean isLegalTouchRegion(float x, float y) {
        if (x <= REGION_WIDTH || x >= mWidth - REGION_WIDTH || y <= REGION_WIDTH || y >= mHeight - REGION_WIDTH) {
            return false;
        }
        RectF rectF = new RectF();
        for (PointF point : mControlPoints) {
            if (mCurPoint != null && mCurPoint.equals(point)) { // 判断是否是当前控制点
                continue;
            }
            rectF.set(point.x - REGION_WIDTH, point.y - REGION_WIDTH, point.x + REGION_WIDTH, point.y + REGION_WIDTH);
            if (rectF.contains(x, y)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取合法控制点
     *
     * @param x
     * @param y
     * @return
     */
    private PointF getLegalControlPoint(float x, float y) {
        RectF rectF = new RectF();
        for (PointF point : mControlPoints) {
            rectF.set(point.x - REGION_WIDTH, point.y - REGION_WIDTH, point.x + REGION_WIDTH, point.y + REGION_WIDTH);
            if (rectF.contains(x, y)) {
                return point;
            }
        }
        return null;
    }


    /**
     * 判断手指坐标是否在合法区域中
     *
     * @param x
     * @param y
     * @return
     */
    private boolean isLegalFingerRegion(float x, float y) {
        if (mCurPoint != null) {
            RectF rectF = new RectF(mCurPoint.x - FINGER_RECT_SIZE / 2, mCurPoint.y - FINGER_RECT_SIZE / 2, mCurPoint
                    .x +
                    FINGER_RECT_SIZE / 2, mCurPoint.y +
                    FINGER_RECT_SIZE / 2);
            if (rectF.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        log("onDraw:" + mRun + ", " + mTouch + ", point:" + mBezierPoint);
        if (mRun && !mTouch) {
            if (mBezierPoint == null) {
                mBezierPath.reset();
                mBezierPoint = mBezierPoints.get(0);
//                log("path reset:" + mBezierPoint);
                mBezierPath.moveTo(mBezierPoint.x, mBezierPoint.y);
            }
            // 控制点和控制点连线
            int size = mControlPoints.size();
            PointF point;
            for (int i = 0; i < size; i++) {
                point = mControlPoints.get(i);
                if (i > 0) {
                    canvas.drawLine(mControlPoints.get(i - 1).x, mControlPoints.get(i - 1).y, point.x, point.y,
                            mLinePaint);
                }
                canvas.drawCircle(point.x, point.y, CONTROL_RADIUS, mControlPaint);
                canvas.drawText("p" + i, point.x + CONTROL_RADIUS * 2, point.y + CONTROL_RADIUS * 2, mTextPaint);
            }

            // 切线
//            canvas.drawLine(mP1.x, mP1.y, mP2.x, mP2.y, mTangentPaint);

            // Bezier曲线
//            log("draw:" + mBezierPoint);
            mBezierPath.lineTo(mBezierPoint.x, mBezierPoint.y);
            canvas.drawPath(mBezierPath, mBezierPaint);
//             Bezier曲线起始移动点
            canvas.drawCircle(mBezierPoint.x, mBezierPoint.y, CONTROL_RADIUS, mMovingPaint);

            mHandler.removeMessages(100);
            mHandler.sendEmptyMessage(100);
        }
        if (mTouch) {
            // 控制点和控制点连线
            int size = mControlPoints.size();
            PointF point;
            for (int i = 0; i < size; i++) {
                point = mControlPoints.get(i);
                if (i > 0) {
                    canvas.drawLine(mControlPoints.get(i - 1).x, mControlPoints.get(i - 1).y, point.x, point.y,
                            mLinePaint);
                }
                canvas.drawCircle(point.x, point.y, CONTROL_RADIUS, mControlPaint);
                canvas.drawText("p" + i, point.x + CONTROL_RADIUS * 2, point.y + CONTROL_RADIUS * 2, mTextPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mTouch) {
            return true;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mRun = false;
                log("down");
                break;
            case MotionEvent.ACTION_MOVE:
                log("move");
                float x = event.getX();
                float y = event.getY();
                if (mCurPoint == null) {
                    mCurPoint = getLegalControlPoint(x, y);
                }
                if (mCurPoint != null && isLegalTouchRegion(x, y)) {  // 判断手指移动区域是否合法
                    if (isLegalFingerRegion(x, y)) {    // 判断手指触摸区域是否合法
                        mCurPoint.x = x;
                        mCurPoint.y = y;
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                log("up");
                mCurPoint = null;
                mRun = true;
                break;
        }
        return true;
    }

    public void start() {
        if (mRun) {
            mBezierPoint = null;
            mBezierPoints = initBezierPoints(mControlPoints);
//            mBezierPoints = createQuad(mControlPoints.get(0), mControlPoints.get(1), mControlPoints.get(2));
            log("run:" + mControlPoints.size() + ", " + mBezierPoints.size());
            mRun = true;
            mTouch = false;
            invalidate();
        }
    }

    private void log(String msg) {
        Log.d("venshine", msg);
    }

}
