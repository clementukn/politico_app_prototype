package com.example.clement.politico;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.clement.politico.PoliticoParser.ArticleItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by clement on 4/11/16.
 */
public class CustomArrayAdapter extends ArrayAdapter<ArticleItem> {
    private Context mContext;
    private int id;
    private List<ArticleItem> items;

    public CustomArrayAdapter(Context context, int textViewResourceId, List<ArticleItem> list) {
        super(context, textViewResourceId, list);
        mContext = context;
        id = textViewResourceId;
        items = list ;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent)
    {
        View mView = v ;
        if (mView == null) {
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = vi.inflate(id, null);
        }

        TextView text = (TextView) mView.findViewById(R.id.textView);

        if(items.get(position) != null ) {
            text.setTextColor(Color.BLACK);
            text.setText(items.get(position).toString());
        }

        return mView;
    }

}