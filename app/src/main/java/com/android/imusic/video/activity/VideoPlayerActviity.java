package com.android.imusic.video.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.android.imusic.R;
import com.android.imusic.base.BaseActivity;
import com.android.imusic.base.BaseEngin;
import com.android.imusic.music.utils.MediaUtils;
import com.android.imusic.video.adapter.VideoDetailsAdapter;
import com.android.imusic.video.bean.OpenEyesIndexItemBean;
import com.android.imusic.video.ui.contract.IndexVideoContract;
import com.android.imusic.video.ui.presenter.IndexVideoPersenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.music.player.lib.adapter.base.OnItemClickListener;
import com.music.player.lib.util.Logger;
import com.music.player.lib.util.MusicUtils;
import com.video.player.lib.base.BaseVideoPlayer;
import com.video.player.lib.base.IMediaPlayer;
import com.video.player.lib.bean.VideoParams;
import com.video.player.lib.constants.VideoConstants;
import com.video.player.lib.controller.DetailsCoverController;
import com.video.player.lib.manager.VideoPlayerManager;
import com.video.player.lib.utils.VideoUtils;
import com.video.player.lib.view.VideoDetailsPlayerTrackView;
import com.video.player.lib.view.VideoTextureView;
import java.util.List;

/**
 * VideoPlayer Activity
 * 视频播放器实例界面
 * Intent 中传递的 VideoConstants.KEY_VIDEO_PLAYING 参数作用：为 true：衔接外部播放任务无缝继续播放,
 * false：不作处理  打开迷你小窗口参见 ID btn_tiny的点击事件示例代码
 */

public class VideoPlayerActviity extends BaseActivity<IndexVideoPersenter>
        implements IndexVideoContract.View {

    private static final String TAG = "VideoPlayerActviity";
    //自动隐藏小窗口按钮倒计时
    public static final int HIDE_VIEW_MILLISS=3000;
    private VideoDetailsPlayerTrackView  mVideoPlayer;
    private VideoDetailsAdapter mAdapter;
    //视频参数
    private VideoParams mVideoParams;
    private boolean mIsPlaying;
    private LinearLayoutManager mLayoutManager;
    private View mBtnTiny;
    private Handler mHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTransparent(false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player_activity);
        VideoPlayerManager.getInstance().setVideoDisplayType(VideoConstants.VIDEO_DISPLAY_TYPE_CUT);
        initViews();
        getIntentParams(getIntent(),true);
    }

    @Override
    protected IndexVideoPersenter createPresenter() {
        return new IndexVideoPersenter();
    }

    private void initViews() {
        //播放器控件宽高
        mVideoPlayer = (VideoDetailsPlayerTrackView) findViewById(R.id.video_player);
        int itemHeight = MusicUtils.getInstance().getScreenWidth(this) * 9 / 16;
        mVideoPlayer.getLayoutParams().height=itemHeight;
        DetailsCoverController coverController = new DetailsCoverController(VideoPlayerActviity.this);
        mVideoPlayer.setVideoCoverController(coverController,false);
        mVideoPlayer.setGlobaEnable(true);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(Math.abs(dy)>0){
                    showViewTpVisible(mBtnTiny);
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                Logger.d(TAG,"onScrollStateChanged-->newState:"+newState);
                if(0==newState&&null!=mHandler){
                    mHandler.removeCallbacksAndMessages(null);
                    mHandler.removeCallbacks(mScrollRunnable);
                    mHandler.postDelayed(mScrollRunnable,HIDE_VIEW_MILLISS);
                }
            }
        });
        recyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new VideoDetailsAdapter(VideoPlayerActviity.this,null);
        //条目点击事件
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, long itemId){
                if(null!=view.getTag()&& view.getTag() instanceof OpenEyesIndexItemBean){
                    OpenEyesIndexItemBean indexItemBean = (OpenEyesIndexItemBean) view.getTag();
                    VideoPlayerManager.getInstance().onReset();
                    Intent intent=new Intent(VideoPlayerActviity.this, VideoPlayerActviity.class);
                    VideoParams videoParams= MediaUtils.getInstance().formatVideoParams(indexItemBean);
                    intent.putExtra(VideoConstants.KEY_VIDEO_PARAMS,videoParams);
                    startActivity(intent);
                }
            }
        });
        recyclerView.setAdapter(mAdapter);
        //退出播放器
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mBtnTiny = findViewById(R.id.btn_tiny);
        //小窗口测试
        mBtnTiny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null!=mVideoPlayer){
                    int startY=mVideoPlayer.getMeasuredHeight()
                            +VideoUtils.getInstance().dpToPxInt(VideoPlayerActviity.this,10f)
                            +VideoUtils.getInstance().getStatusBarHeight(VideoPlayerActviity.this);
                    mVideoPlayer.startMiniWindowToLocaion(Gravity.RIGHT,startY,1280,720,null);
                }
            }
        });
        mHandler = new Handler();
        //两秒后自动隐藏小窗口转向
        mHandler.postDelayed(mScrollRunnable,HIDE_VIEW_MILLISS);
    }

    /**
     * 显示小窗口View
     * @param view
     */
    private void showViewTpVisible(View view) {
        if(null!=mHandler){
            mHandler.removeCallbacks(mScrollRunnable);
        }
        if(null!=view){
            if(view.getVisibility()!=View.VISIBLE){
                view.setVisibility(View.VISIBLE);
                ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", view.getWidth(),0);
                animator.setDuration(300);
                animator.setInterpolator(new LinearInterpolator());
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if(null!=mHandler){
                            mHandler.removeCallbacks(mScrollRunnable);
                            mHandler.postDelayed(mScrollRunnable,HIDE_VIEW_MILLISS);
                        }
                    }
                });
                animator.start();
            }
        }
    }

    /**
     * 隐藏小窗口View
     * @param view
     */
    private void hideViewTpGone(final View view) {
        if(null!=view){
            if(view.getVisibility()==View.GONE){
                return;
            }
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationX", 0,view.getMeasuredWidth());
            animator.setDuration(300);
            animator.setInterpolator(new LinearInterpolator());
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    view.setVisibility(View.GONE);
                }
            });
            animator.start();
        }
    }

    /**
     * token任务
     */
    public Runnable mScrollRunnable=new Runnable() {
        @Override
        public void run() {
            hideViewTpGone(mBtnTiny);
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getIntentParams(intent,false);
    }

    /**
     * 获取视频入参
     * @param intent
     * @param isCreate
     */
    private void getIntentParams(Intent intent,boolean isCreate) {
        if(null==intent) return;
        mVideoParams = intent.getParcelableExtra(VideoConstants.KEY_VIDEO_PARAMS);
        mIsPlaying = intent.getBooleanExtra(VideoConstants.KEY_VIDEO_PLAYING,false);
        if(null!=mAdapter&&mAdapter.getData().size()>0){
            mAdapter.getData().get(0).setVideoParams(mVideoParams);
            mAdapter.notifyDataSetChanged();
        }
        if(null==mVideoParams){
            Toast.makeText(VideoPlayerActviity.this,"缺少必要参数",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if(TextUtils.isEmpty(mVideoParams.getVideoUrl())){
            Toast.makeText(VideoPlayerActviity.this,"缺少必要参数",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        initVideoParams(isCreate);
    }

    /**
     * 播放器初始化
     * @param isCreate
     */
    private void initVideoParams(boolean isCreate) {
        if(null!=mVideoParams){
            mVideoPlayer.setDataSource(mVideoParams.getVideoUrl(),mVideoParams.getVideoTitle(),mVideoParams.getVideoiId());
            mVideoPlayer.setPlayerWorking(true);
            mVideoPlayer.setParamsTag(mVideoParams);
            //封面
            if(null!=mVideoPlayer.getCoverController()){
                Glide.with(VideoPlayerActviity.this)
                        .load(mVideoParams.getVideoCover())
                        .placeholder(R.drawable.ic_video_default_cover)
                        .error(R.drawable.ic_video_default_cover)
                        .dontAnimate()
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(mVideoPlayer.getCoverController().mVideoCover);
            }
            //无缝衔接外部播放任务
            if(mIsPlaying&&null!= IMediaPlayer.getInstance().getTextureView()){
                addTextrueViewToView(mVideoPlayer);
                IMediaPlayer.getInstance().addOnPlayerEventListener(mVideoPlayer);
                //手动检查播放器内部状态，同步常规播放器状态至全屏播放器
                IMediaPlayer.getInstance().checkedVidepPlayerState();
            }else{
                //开始全新播放任务
                mVideoPlayer.startPlayVideo();
            }
            if(null!=mPresenter&&!TextUtils.isEmpty(mVideoParams.getVideoiId())){
                //获取推荐视频
                mPresenter.getVideosByVideo(mVideoParams.getVideoiId());
            }
        }
    }


    /**
     * 添加一个视频渲染组件至View
     * @param videoPlayer
     */
    private void addTextrueViewToView(BaseVideoPlayer videoPlayer) {
        //先移除存在的TextrueView
        if(null!=IMediaPlayer.getInstance().getTextureView()){
            VideoTextureView textureView = IMediaPlayer.getInstance().getTextureView();
            if(null!=textureView.getParent()){
                ((ViewGroup) textureView.getParent()).removeView(textureView);
            }
        }
        if(null!=IMediaPlayer.getInstance().getTextureView()){
            videoPlayer.mSurfaceView.addView(IMediaPlayer.getInstance().getTextureView(),
                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        }
    }

    @Override
    public void showLoading() {

    }

    @Override
    public void showError(int code, String errorMsg) {
        if(!VideoPlayerActviity.this.isFinishing()){
            if(code== BaseEngin.API_RESULT_EMPTY){
                mAdapter.onLoadEnd();
            }else{
                mAdapter.onLoadError();
            }
            Toast.makeText(VideoPlayerActviity.this,errorMsg,Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示视频列表
     * @param data 视频列表
     * @param isRestart 是否从第一页开始加载的
     */
    @Override
    public void showVideos(List<OpenEyesIndexItemBean> data, boolean isRestart) {
        if(!VideoPlayerActviity.this.isFinishing()){
            if(null!=mAdapter){
                mAdapter.onLoadComplete();
                OpenEyesIndexItemBean openEyesIndexItemBean=new OpenEyesIndexItemBean();
                openEyesIndexItemBean.setType(VideoConstants.VIDEO_HEADER);
                openEyesIndexItemBean.setVideoParams(mVideoParams);
                data.add(0,openEyesIndexItemBean);
                mAdapter.setNewData(data);
                if(null!=mLayoutManager){
                    mLayoutManager.scrollToPositionWithOffset(0,0);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoPlayerManager.getInstance().onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        VideoPlayerManager.getInstance().onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if(VideoPlayerManager.getInstance().isBackPressed()){
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VideoPlayerManager.getInstance().onDestroy();
        if(null!=mAdapter){
            mAdapter.onDestroy();
            mAdapter=null;
        }
        if(null!=mVideoPlayer){
            mVideoPlayer.onDestroy();
            mVideoPlayer=null;
        }
        if(null!=mHandler&&null!=mScrollRunnable){
            mHandler.removeCallbacksAndMessages(null);
            mHandler.removeCallbacks(mScrollRunnable);
            mHandler=null;
        }
        mLayoutManager=null;mVideoParams=null;
    }
}