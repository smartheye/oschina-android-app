package net.oschina.app.improve.tweet.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.TextHttpResponseHandler;

import net.oschina.app.R;
import net.oschina.app.api.remote.OSChinaApi;
import net.oschina.app.bean.Constants;
import net.oschina.app.cache.CacheManager;
import net.oschina.app.improve.account.AccountHelper;
import net.oschina.app.improve.account.activity.LoginActivity;
import net.oschina.app.improve.app.AppOperator;
import net.oschina.app.improve.base.adapter.BaseGeneralRecyclerAdapter;
import net.oschina.app.improve.base.adapter.BaseRecyclerAdapter;
import net.oschina.app.improve.base.fragments.BaseGeneralRecyclerFragment;
import net.oschina.app.improve.bean.Tweet;
import net.oschina.app.improve.bean.base.PageBean;
import net.oschina.app.improve.bean.base.ResultBean;
import net.oschina.app.improve.tweet.activities.TweetDetailActivity;
import net.oschina.app.improve.user.adapter.UserTweetAdapter;
import net.oschina.app.improve.utils.DialogHelper;
import net.oschina.app.ui.empty.EmptyLayout;
import net.oschina.app.util.HTMLUtil;
import net.oschina.app.util.TDevice;

import org.json.JSONObject;

import java.lang.reflect.Type;

import cz.msebera.android.httpclient.Header;

import static net.oschina.app.improve.tweet.activities.TweetDetailActivity.BUNDLE_KEY_TWEET;

/**
 * 动弹列表
 * Created by huanghaibin_dev
 * on 2016/7/18.
 */
public class TweetFragment extends BaseGeneralRecyclerFragment<Tweet> {
    public static final int CATEGORY_TYPE = 1; //请求最新或者最热
    public static final int CATEGORY_USER = 2; //请求用户

    public static final int TWEET_TYPE_NEW = 1;
    public static final int TWEET_TYPE_HOT = 2;

    public static final String CACHE_NEW_TWEET = "cache_new_tweet";
    public static final String CACHE_HOT_TWEET = "cache_hot_tweet";
    public static final String CACHE_USER_TWEET = "cache_user_tweet";

    public int requestCategory;//请求类型
    public int tweetType;
    public long authorId;

    public static Fragment instantiate(long aid) {
        Bundle bundle = new Bundle();
        bundle.putLong("authorId", aid);
        bundle.putInt("requestCategory", CATEGORY_USER);
        Fragment fragment = new TweetFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected void initBundle(Bundle bundle) {
        super.initBundle(bundle);
        requestCategory = bundle.getInt("requestCategory", CATEGORY_TYPE);
        tweetType = bundle.getInt("tweetType", TWEET_TYPE_NEW);
        authorId = bundle.getLong("authorId", AccountHelper.getUserId());
    }

    /**
     * fragment被销毁的时候重新调用，初始化保存的数据
     *
     * @param bundle onSaveInstanceState
     */
    @Override
    protected void onRestartInstance(Bundle bundle) {
        super.onRestartInstance(bundle);
        requestCategory = bundle.getInt("requestCategory", 1);
        tweetType = bundle.getInt("tweetType", 1);
        authorId = bundle.getLong("authorId", AccountHelper.getUserId());
    }

    @Override
    public void initData() {
        super.initData();
        switch (requestCategory) {
            case CATEGORY_TYPE:
                CACHE_NAME = tweetType == TWEET_TYPE_NEW ? CACHE_NEW_TWEET : CACHE_HOT_TWEET;
                break;
            case CATEGORY_USER:
                CACHE_NAME = CACHE_USER_TWEET;
                IntentFilter filter = new IntentFilter(
                        Constants.INTENT_ACTION_USER_CHANGE);
                filter.addAction(Constants.INTENT_ACTION_LOGOUT);
                if (mReceiver == null) {
                    mReceiver = new LoginReceiver();
                    getActivity().registerReceiver(mReceiver, filter);
                }
                break;
        }

        mAdapter.setOnItemLongClickListener(new BaseRecyclerAdapter.OnItemLongClickListener() {
            @Override
            public void onLongClick(int position, long itemId) {
                Tweet tweet = mAdapter.getItem(position);
                if (tweet != null) {
                    handleLongClick(tweet, position);
                }
            }
        });

        if (authorId == 0 && requestCategory == CATEGORY_USER) {
            mErrorLayout.setErrorType(EmptyLayout.NETWORK_ERROR);
            mErrorLayout.setErrorMessage(getString(R.string.unlogin_tip));

        }
    }

    private class LoginReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            setupContent();
        }
    }

    private LoginReceiver mReceiver;

    private void setupContent() {
        if (AccountHelper.isLogin()) {
            authorId = AccountHelper.getUserId();
            mErrorLayout.setErrorType(EmptyLayout.NETWORK_LOADING);
            onRefreshing();
        } else {
            authorId = 0;
            mErrorLayout.setErrorType(EmptyLayout.NETWORK_ERROR);
            mErrorLayout.setErrorMessage(getString(R.string.unlogin_tip));
        }
    }


    @Override
    protected void requestData() {
        super.requestData();
        switch (requestCategory) {
            case CATEGORY_TYPE:
                OSChinaApi.getTweetList(tweetType, mIsRefresh ? null : mBean.getNextPageToken(), mHandler);
                break;
            case CATEGORY_USER:
                if (authorId != 0) {
                    OSChinaApi.getUserTweetList(authorId, mIsRefresh ? null : mBean.getNextPageToken(), mHandler);
                }
                break;
        }
    }

    @Override
    public void onItemClick(int position, long itemId) {
        UserTweetAdapter.ViewHolder holder = (UserTweetAdapter.ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
        if (holder == null) return;

        ImageView mp = (ImageView) holder.itemView.findViewById(R.id.iv_tweet_face);
        TextView mn = (TextView) holder.itemView.findViewById(R.id.tv_tweet_name);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                Pair.<View, String>create(mp, ViewCompat.getTransitionName(mp)),
                Pair.<View, String>create(mn, ViewCompat.getTransitionName(mn))
        );

        Intent intent = new Intent(getContext(), TweetDetailActivity.class);
        intent.putExtra(BUNDLE_KEY_TWEET, mAdapter.getItem(position));
        ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
    }

    @Override
    public void onClick(View v) {
        if (requestCategory == CATEGORY_USER && !AccountHelper.isLogin()) {
            //UIHelper.showLoginActivity(getActivity());
            LoginActivity.show(getContext());
        } else {
            super.onClick(v);
        }
    }

    @Override
    protected void setListData(ResultBean<PageBean<Tweet>> resultBean) {
        mBean.setNextPageToken((resultBean == null ? null : resultBean.getResult().getNextPageToken()));
        if (mIsRefresh) {
            //cache the time
            mBean.setItems(resultBean.getResult().getItems());
            mAdapter.clear();

            ((BaseGeneralRecyclerAdapter) mAdapter).clearPreItems();
            ((BaseGeneralRecyclerAdapter) mAdapter).addItems(mBean.getItems());

            mBean.setPrevPageToken((resultBean == null ? null : resultBean.getResult().getPrevPageToken()));
            mRefreshLayout.setCanLoadMore(true);
            if (isNeedCache()) {
                AppOperator.runOnThread(new Runnable() {
                    @Override
                    public void run() {
                        CacheManager.saveObject(getActivity(), mBean, CACHE_NAME);
                    }
                });
            }
        } else {
            ((BaseGeneralRecyclerAdapter) mAdapter).addItems(resultBean.getResult().getItems());
        }

        mAdapter.setState(resultBean.getResult().getItems() == null || resultBean.getResult().getItems().size() < 20 ? BaseRecyclerAdapter.STATE_NO_MORE : BaseRecyclerAdapter.STATE_LOADING, true);

        if (mAdapter.getItems().size() > 0) {
            mErrorLayout.setErrorType(EmptyLayout.HIDE_LAYOUT);
            mRefreshLayout.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
        } else {
            mErrorLayout.setErrorType(isNeedEmptyView() ? EmptyLayout.NODATA : EmptyLayout.HIDE_LAYOUT);
        }
    }

    private void handleLongClick(final Tweet tweet, final int position) {
        String[] items;
        if (AccountHelper.getUserId() == (int) tweet.getAuthor().getId()) {
            items = new String[]{getString(R.string.copy),
                    getString(R.string.delete)};
        } else {
            items = new String[]{getString(R.string.copy)};
        }

        DialogHelper.getSelectDialog(getActivity(), items, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                switch (i) {
                    case 0:
                        TDevice.copyTextToBoard(HTMLUtil.delHTMLTag(tweet.getContent()));
                        break;
                    case 1:
                        DialogHelper.getConfirmDialog(getActivity(), "是否删除该动弹?", new DialogInterface
                                .OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                OSChinaApi.deleteTweet(tweet.getId(), new DeleteHandler(position));
                            }
                        }).show();
                        break;
                }
            }
        }).show();
    }


    @Override
    protected BaseRecyclerAdapter<Tweet> getRecyclerAdapter() {
        return new UserTweetAdapter(this, BaseRecyclerAdapter.ONLY_FOOTER);
    }

    @Override
    protected Type getType() {
        return new TypeToken<ResultBean<PageBean<Tweet>>>() {
        }.getType();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("requestCategory", requestCategory);
        outState.putInt("tweetType", tweetType);
        outState.putLong("authorId", authorId);
        super.onSaveInstanceState(outState);
    }

    /**
     * 注销广播
     */
    @Override
    public void onDestroy() {
        if (requestCategory == CATEGORY_USER && mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }

    class DeleteHandler extends TextHttpResponseHandler {
        private int position;

        DeleteHandler(int position) {
            this.position = position;
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, String responseString) {
            try {
                JSONObject jsonObject = new JSONObject(responseString);
                if (jsonObject.optInt("code") == 1) {
                    mAdapter.removeItem(position);
                    Toast.makeText(getActivity(), "删除成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), jsonObject.optString("message"), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
