package net.oschina.app.fragment.base;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import net.oschina.app.AppConfig;
import net.oschina.app.AppContext;
import net.oschina.app.R;
import net.oschina.app.adapter.base.BaseListAdapter;
import net.oschina.app.bean.base.PageBean;
import net.oschina.app.bean.base.ResultBean;
import net.oschina.app.cache.CacheManager;
import net.oschina.app.widget.SuperRefreshLayout;

import java.lang.reflect.Type;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

/**
 * T as the base bean
 * Created by huanghaibin
 * on 16-5-23.
 */
public abstract class BaseListFragment<T> extends BaseFragment implements
        SuperRefreshLayout.SuperRefreshLayoutListener, AdapterView.OnItemClickListener, BaseListAdapter.Callback {

    protected final String CACHE_NAME = getClass().getName();
    private String mTime;

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_LOADING = 1;
    public static final int TYPE_NO_MORE = 2;
    public static final int TYPE_ERROR = 3;
    public static final int TYPE_NET_ERROR = 4;

    private View mFooterView;
    private ProgressBar mFooterProgressBar;
    private TextView mFooterText;

    protected ListView mListView;

    protected SuperRefreshLayout mRefreshLayout;

    protected BaseListAdapter<T> mAdapter;
    protected boolean mIsRefresh;

    protected TextHttpResponseHandler mHandler;

    private PageBean<T> mBeam;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_base_list;
    }

    @Override
    protected void initWidget(View root) {
        super.initWidget(root);
        mListView = (ListView) root.findViewById(R.id.listView);
        mRefreshLayout = (SuperRefreshLayout) root.findViewById(R.id.superRefreshLayout);
        mFooterView = LayoutInflater.from(getContext()).inflate(R.layout.layout_list_view_footer, null);
        mFooterText = (TextView) mFooterView.findViewById(R.id.tv_footer);
        mFooterProgressBar = (ProgressBar) mFooterView.findViewById(R.id.pb_footer);
        setFooterType(TYPE_LOADING);
        if (isNeedFooter())
            mListView.addFooterView(mFooterView);
    }

    @Override
    protected void initData() {
        super.initData();
        //when open this fragment,read the obj
        mBeam = (PageBean<T>) CacheManager.readObject(getActivity(), CACHE_NAME);
        mAdapter = getListAdapter();
        mListView.setAdapter(mAdapter);

        mHandler = new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                onRequestError(statusCode);
                onRequestFinish();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                try {
                    ResultBean<PageBean<T>> resultBean = AppContext.createGson().fromJson(responseString, getType());
                    if (resultBean != null) {
                        onRequestSuccess(resultBean.getCode());
                        setListData(resultBean);
                    }
                    //// TODO: 16-5-23
                    onComplete();
                    onRequestFinish();
                } catch (Exception e) {
                    e.printStackTrace();
                    onFailure(statusCode, headers, responseString, e);
                }
            }
        };
        //if is the first loading
        if (mBeam == null) {
            mBeam = new PageBean<>();
            onRefreshing();
        } else {
            mAdapter.addItem(mBeam.getItems());
            // not ExpiryDate
            if (!AppConfig.isExpiryDate(AppConfig.getAppConfig(getActivity()).get(CACHE_NAME))) {
                onRefreshing();
            }
        }
    }

    @Override
    public void onRefreshing() {
        mIsRefresh = true;
        requestData();
    }

    @Override
    public void onLoadMore() {
        requestData();
    }

    /**
     * request network data
     */
    protected void requestData() {
        onRequestStart();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        T item = mAdapter.getItem(position);
        if (item != null)
            onItemClick(item, position);
    }

    protected void onItemClick(T item, int position) {

    }

    protected void onRequestStart() {

    }

    protected void onRequestSuccess(int code) {

    }

    protected void onRequestError(int code) {

    }

    protected void onRequestFinish() {

    }

    protected void onComplete() {
        mRefreshLayout.onLoadComplete();
    }

    protected void setListData(ResultBean<PageBean<T>> resultBean) {
        //is refresh
        if (mIsRefresh) {
            //cache the time
            mTime = resultBean.getTime();
            AppConfig.getAppConfig(getActivity()).set(CACHE_NAME, mTime);

            //is ExpiryDate
            if (AppConfig.isExpiryDate(mTime)) {
                mBeam.getItems().addAll(0, resultBean.getResult().getItems());
                mAdapter.addItem(0, resultBean.getResult().getItems());
            } else {
                mBeam.setItems(resultBean.getResult().getItems());
                mAdapter.clear();
                mAdapter.addItem(mBeam.getItems());
            }
            mBeam.setNextPageToken(resultBean.getResult().getNextPageToken());
            mBeam.setPrevPageToken(resultBean.getResult().getPrevPageToken());
            CacheManager.saveObject(getActivity(), mBeam, CACHE_NAME);
        } else {
            mAdapter.addItem(resultBean.getResult().getItems());
        }
    }

    @Override
    public Date getSystemTime() {
        return new Date();
    }

    protected abstract BaseListAdapter<T> getListAdapter();

    protected abstract Type getType();

    protected boolean isNeedFooter() {
        return true;
    }

    protected void setFooterType(int type) {
        switch (type) {
            case TYPE_NORMAL:
            case TYPE_LOADING:
                mFooterText.setText(getResources().getString(R.string.footer_type_loading));
                mFooterProgressBar.setVisibility(View.VISIBLE);
                break;
            case TYPE_NET_ERROR:
                mFooterText.setText(getResources().getString(R.string.footer_type_net_error));
                mFooterProgressBar.setVisibility(View.GONE);
                break;
            case TYPE_ERROR:
                mFooterText.setText(getResources().getString(R.string.footer_type_error));
                mFooterProgressBar.setVisibility(View.GONE);
                break;
            case TYPE_NO_MORE:
                mFooterText.setText(getResources().getString(R.string.footer_type_not_more));
                mFooterProgressBar.setVisibility(View.GONE);
                break;
        }
    }
}
