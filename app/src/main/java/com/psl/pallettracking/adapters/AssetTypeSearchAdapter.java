package com.psl.pallettracking.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.psl.pallettracking.R;
import com.psl.pallettracking.helper.AppConstants;

import java.util.ArrayList;

import gr.escsoft.michaelprimez.searchablespinner.interfaces.ISpinnerSelectedView;

public class AssetTypeSearchAdapter extends ArrayAdapter<String> implements Filterable, ISpinnerSelectedView {

    private Context mContext;
    private ArrayList<String> mBackupStrings;
    private ArrayList<String> mStrings;
    private StringFilter mStringFilter = new StringFilter();

    public AssetTypeSearchAdapter(Context context,ArrayList<String> equipmentList) {
        super(context, R.layout.asset_type_search_spinner_item_view);
        mContext = context;
        mStrings = equipmentList;
        mBackupStrings = equipmentList;
    }

    @Override
    public int getCount() {
        return mStrings == null ? 0 : mStrings.size() + 1;
    }

    @Override
    public String getItem(int position) {
        if (mStrings != null && position > 0)
            return mStrings.get(position - 1);
        else
            return null;
    }

    @Override
    public long getItemId(int position) {
        if (mStrings == null && position > 0)
            return mStrings.get(position).hashCode();
        else
            return -1;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = null;
        if (position == 0) {
            view = getNoSelectionView();
        } else {
            view = View.inflate(mContext, R.layout.asset_type_search_spinner_item_view, null);
            // ImageView letters = (ImageView) view.findViewById(R.id.ImgVw_Letters);
            TextView dispalyName = (TextView) view.findViewById(R.id.textEquipmentNumber);
            //letters.setImageDrawable(getTextDrawable(mStrings.get(position-1)));
            String eqnumber = mStrings.get(position-1).split(AppConstants.ASSET_TYPE_SPLIT_DATA)[0];
            dispalyName.setText(eqnumber);
            //dispalyName.setText(mStrings.get(position-1));
        }
        return view;

    }


    @Override
    public View getSelectedView(int position) {
        View view = null;
        if (position == 0) {
            view = getNoSelectionView();
        } else {
            view = View.inflate(mContext, R.layout.asset_type_search_spinner_item_view, null);
            //ImageView letters = (ImageView) view.findViewById(R.id.ImgVw_Letters);
            TextView dispalyName = (TextView) view.findViewById(R.id.textEquipmentNumber);
            //letters.setImageDrawable(getTextDrawable(mStrings.get(position-1)));
            String eqnumber = mStrings.get(position-1).split(AppConstants.ASSET_TYPE_SPLIT_DATA)[0];
            dispalyName.setText(eqnumber);
        }
        return view;
    }


    @Override
    public View getNoSelectionView() {
        View view = View.inflate(mContext, R.layout.asset_type_search_adapter_view_list_no_selection_item, null);
        //TxtVw_NoSelection
        TextView TxtVw_NoSelection = (TextView) view.findViewById(R.id.TxtVw_NoSelection);
        if(mStrings!=null){
            if(mStrings.size()>0){
                TxtVw_NoSelection.setText("Select Asset");
                TxtVw_NoSelection.setTextColor(mContext.getResources().getColor(R.color.black));
            }else{
                TxtVw_NoSelection.setText("No Data Found");
                TxtVw_NoSelection.setTextColor(mContext.getResources().getColor(R.color.red));
            }
        }
        return view;
    }

    @Override
    public Filter getFilter() {
        return mStringFilter;
    }

    public class StringFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            constraint = constraint.toString().toUpperCase();
            final FilterResults filterResults = new FilterResults();
            if (TextUtils.isEmpty(constraint)) {
                filterResults.count = mBackupStrings.size();
                filterResults.values = mBackupStrings;
                return filterResults;
            }
            final ArrayList<String> filterStrings = new ArrayList<>();
            for (String text : mBackupStrings) {
                if (text.toUpperCase().contains(constraint)) {
                    filterStrings.add(text);
                }
            }
            filterResults.count = filterStrings.size();
            filterResults.values = filterStrings;
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mStrings = (ArrayList) results.values;
            notifyDataSetChanged();
        }
    }

}
