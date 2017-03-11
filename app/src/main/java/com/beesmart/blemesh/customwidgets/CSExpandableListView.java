package com.beesmart.blemesh.customwidgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ExpandableListView;

public class CSExpandableListView extends ExpandableListView {

	private int mMaxHeight = -1;

	public CSExpandableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CSExpandableListView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setMaxHeight(int maxHeight) {
		this.mMaxHeight = maxHeight;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (mMaxHeight > -1) {
			heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

}
