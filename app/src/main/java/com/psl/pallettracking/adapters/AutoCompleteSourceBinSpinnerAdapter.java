package com.psl.pallettracking.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.psl.pallettracking.BinPartialPalletMapProcessActivity;
import com.psl.pallettracking.R;

import java.util.List;

public class AutoCompleteSourceBinSpinnerAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater inflater;
    private List<BinPartialPalletMappingCreationProcessModel> lst;

    public AutoCompleteSourceBinSpinnerAdapter(Context context, List<BinPartialPalletMappingCreationProcessModel> lst) {
        this.context = context;
        this.lst = lst;
        inflater = LayoutInflater.from(this.context);
    }
    @Override
    public int getCount() {
        return lst.size();
    }

    @Override
    public Object getItem(int position) {
        return lst.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        MyViewHolder mViewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.bin_partial_pallet_mapping_workorder_process_adapter, parent, false);
            mViewHolder = new MyViewHolder(convertView);
            convertView.setTag(mViewHolder);
        } else {
            mViewHolder = (MyViewHolder) convertView.getTag();
        }
        //  SFModal crm = lst.get(position);//change by bapu 5-sep-2018 to desc order
        final BinPartialPalletMappingCreationProcessModel objectItem = lst.get(position);
        mViewHolder.textItemDesc.setText(objectItem.getBinDescription());
        mViewHolder.textItemDesc.setSelected(true);
        mViewHolder.textBinName.setText(objectItem.getBatchId());
        mViewHolder.textBinName.setSelected(true);
        if(position==0){
            mViewHolder.textPickQty.setText("Qty");
        }else{
            mViewHolder.textPickQty.setText(""+objectItem.getPickedQty());
        }

        return convertView;
    }

    private class MyViewHolder {
        TextView textItemDesc,textBinName,textPickQty;
        public MyViewHolder(View convertView) {
            textItemDesc = (TextView) convertView.findViewById(R.id.textItemDesc);
            textBinName = (TextView) convertView.findViewById(R.id.textBinName);
            textPickQty = (TextView) convertView.findViewById(R.id.textPickQty);
        }
    }
}
/*extends ArrayAdapter<BinPartialPalletMappingCreationProcessModel> {

    final String TAG = "AutoCompleteSourceBinSpinnerAdapter.java";
    Context mContext;
    int layoutResourceId;
    List<BinPartialPalletMappingCreationProcessModel> data = null;


    public AutoCompleteSourceBinSpinnerAdapter(Context mContext, int layoutResourceId, List<BinPartialPalletMappingCreationProcessModel> data) {

        super(mContext, layoutResourceId, data);

        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
        this.data = data;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        try{

            *//*
             * The convertView argument is essentially a "ScrapView" as described is Lucas post
             * http://lucasr.org/2012/04/05/performance-tips-for-androids-listview/
             * It will have a non-null value when ListView is asking you recycle the row layout.
             * So, when convertView is not null, you should simply update its contents instead of inflating a new row layout.
             *//*
            if(convertView==null){
                // inflate the layout
                LayoutInflater inflater = ((BinPartialPalletMapProcessActivity) mContext).getLayoutInflater();
                convertView = inflater.inflate(layoutResourceId, parent, false);
            }

            // object item based on the position
            BinPartialPalletMappingCreationProcessModel objectItem = data.get(position);

            // get the TextView and then set the text (item name) and tag (item ID) values
            TextView textItemDesc = (TextView) convertView.findViewById(R.id.textItemDesc);
            TextView textBinName = (TextView) convertView.findViewById(R.id.textBinName);
            TextView textPickQty = (TextView) convertView.findViewById(R.id.textPickQty);
            // textViewItem.setText(objectItem.objectName);
            textItemDesc.setText(objectItem.getBinDescription());
            textBinName.setText(objectItem.getBatchId());
            textPickQty.setText(objectItem.getPickedQty());


        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return convertView;

    }
}*/
