package net.oschina.app.improve.main.synthesize.detail;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import net.oschina.app.OSCApplication;
import net.oschina.app.R;
import net.oschina.app.improve.base.BaseRecyclerFragment;
import net.oschina.app.improve.base.adapter.BaseRecyclerAdapter;
import net.oschina.app.improve.bean.Article;
import net.oschina.app.improve.bean.News;
import net.oschina.app.improve.bean.comment.Comment;
import net.oschina.app.improve.bean.simple.Author;
import net.oschina.app.improve.detail.general.BlogDetailActivity;
import net.oschina.app.improve.detail.general.EventDetailActivity;
import net.oschina.app.improve.detail.general.NewsDetailActivity;
import net.oschina.app.improve.detail.general.QuestionDetailActivity;
import net.oschina.app.improve.detail.general.SoftwareDetailActivity;
import net.oschina.app.improve.main.synthesize.DataFormat;
import net.oschina.app.improve.main.synthesize.article.ArticleAdapter;
import net.oschina.app.improve.main.synthesize.web.ArticleWebActivity;
import net.oschina.app.improve.media.ImageGalleryActivity;
import net.oschina.app.improve.widget.PortraitView;
import net.oschina.app.util.UIHelper;

/**
 * 文章详情
 * Created by huanghaibin on 2017/10/27.
 */

public class ArticleDetailFragment extends BaseRecyclerFragment<ArticleDetailContract.Presenter, Article>
        implements ArticleDetailContract.View,
        View.OnClickListener {
    private OSCApplication.ReadState mReadState;
    protected CommentView mCommentView;
    private Article mArticle;
    private View mHeaderView;

    public static ArticleDetailFragment newInstance(Article article) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("article", article);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_article;
    }


    @Override
    protected void initBundle(Bundle bundle) {
        super.initBundle(bundle);
        mArticle = (Article) bundle.getSerializable("article");
    }

    @SuppressLint("InflateParams,CutPasteId")
    @Override
    protected void initData() {
        mReadState = OSCApplication.getReadState("sub_list");
        mHeaderView = mInflater.inflate(R.layout.layou_article_header, null);
        mAdapter.setHeaderView(mHeaderView);
        ImageView imageView = (ImageView) mHeaderView.findViewById(R.id.iv_article);
        FrameLayout frameLayout = (FrameLayout) mHeaderView.findViewById(R.id.fl_img);
        if (mArticle.getImgs() != null && mArticle.getImgs().length != 0) {
            imageView.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.VISIBLE);
            getImgLoader().load(mArticle.getImgs()[0])
                    .centerCrop()
                    .into(imageView);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImageGalleryActivity.show(mContext, mArticle.getImgs()[0]);
                }
            });
        }
        TextView tv_title = (TextView) mHeaderView.findViewById(R.id.tv_title);
        TextView tv_name = (TextView) mHeaderView.findViewById(R.id.tv_name);
        TextView tv_pub_date = (TextView) mHeaderView.findViewById(R.id.tv_pub_date);
        TextView tv_origin = (TextView) mHeaderView.findViewById(R.id.tv_origin);
        TextView tv_detail_abstract = (TextView) mHeaderView.findViewById(R.id.tv_detail_abstract);
        tv_title.setText(mArticle.getTitle());
        tv_name.setText(TextUtils.isEmpty(mArticle.getAuthorName()) ? "匿名" : mArticle.getAuthorName());
        tv_pub_date.setText(DataFormat.parsePubDate(mArticle.getPubDate()));
        tv_detail_abstract.setText(mArticle.getDesc());
        PortraitView portraitView = (PortraitView) mHeaderView.findViewById(R.id.iv_avatar);
        tv_origin.setText(mArticle.getSourceName());
        Author author = new Author();
        author.setName(mArticle.getAuthorName());
        portraitView.setup(author);
        mCommentView = (CommentView) mHeaderView.findViewById(R.id.commentView);
        mCommentView.setTitle("热门评论");
        mCommentView.init(mArticle, mArticle.getKey(), 1, (CommentView.OnCommentClickListener) mContext);
        mHeaderView.findViewById(R.id.btn_read_all).setOnClickListener(this);
        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.setRefreshing(false);
                if (mPresenter == null)
                    return;
                mPresenter.onRefreshing();
            }
        });
    }

    @Override
    public void onRefreshing() {
        super.onRefreshing();
        mCommentView.init(mArticle, mArticle.getKey(), 1, (CommentView.OnCommentClickListener) mContext);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_read_all:
                ArticleWebActivity.show(mContext, mArticle);
                break;
        }
    }

    @Override
    protected void onItemClick(Article top, int position) {
        if (top.getType() == 0) {
            ArticleDetailActivity.show(mContext, top);
        } else {
            try {
                int type = top.getType();
                long id = top.getOscId();
                switch (type) {
                    case News.TYPE_SOFTWARE:
                        SoftwareDetailActivity.show(mContext, id);
                        break;
                    case News.TYPE_QUESTION:
                        QuestionDetailActivity.show(mContext, id);
                        break;
                    case News.TYPE_BLOG:
                        BlogDetailActivity.show(mContext, id);
                        break;
                    case News.TYPE_TRANSLATE:
                        NewsDetailActivity.show(mContext, id);
                        break;
                    case News.TYPE_EVENT:
                        EventDetailActivity.show(mContext, id);
                        break;
                    case News.TYPE_NEWS:
                        NewsDetailActivity.show(mContext, id);
                        break;
                    default:
                        UIHelper.showUrlRedirect(mContext, top.getUrl());
                        break;
                }

            } catch (Exception e) {
                e.printStackTrace();
                ArticleDetailActivity.show(mContext, top);
            }
        }
        mReadState.put(top.getKey());
        mAdapter.updateItem(position);
    }

    @Override
    public void showCommentSuccess(Comment comment) {
        
    }

    @Override
    public void showCommentError(String message) {

    }

    @Override
    public void onComplete() {
        super.onComplete();
        if (mContext == null)
            return;
        hideOrShowTitle(mAdapter.getItems().size() != 0);
    }

    private void hideOrShowTitle(boolean isShow) {
        if (isShow) {
            mHeaderView.findViewById(R.id.line1).setVisibility(View.VISIBLE);
            mHeaderView.findViewById(R.id.line2).setVisibility(View.VISIBLE);
            mHeaderView.findViewById(R.id.tv_blog_detail_comment).setVisibility(View.VISIBLE);
            mAdapter.setState(BaseRecyclerAdapter.STATE_LOADING,true);
        } else {
            mHeaderView.findViewById(R.id.line1).setVisibility(View.GONE);
            mHeaderView.findViewById(R.id.line2).setVisibility(View.GONE);
            mHeaderView.findViewById(R.id.tv_blog_detail_comment).setVisibility(View.GONE);
            mAdapter.setState(BaseRecyclerAdapter.STATE_HIDE,true);
        }
    }

    @Override
    protected BaseRecyclerAdapter<Article> getAdapter() {
        return new ArticleAdapter(mContext, BaseRecyclerAdapter.BOTH_HEADER_FOOTER);
    }
}
