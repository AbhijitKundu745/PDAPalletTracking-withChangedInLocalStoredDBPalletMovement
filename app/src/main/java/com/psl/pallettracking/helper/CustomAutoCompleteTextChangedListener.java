package com.psl.pallettracking.helper;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;

import com.psl.pallettracking.AssetSearchActivity;
import com.psl.pallettracking.R;
import com.psl.pallettracking.adapters.AutocompleteCustomArrayAdapter;
import com.psl.pallettracking.adapters.MyObject;

/**
 * Created by Admin on 18/Sep/2018.
 */

public class CustomAutoCompleteTextChangedListener implements TextWatcher {

    public static final String TAG = "CustomAutoCompleteTextChangedListener.java";
    Context context;

    public CustomAutoCompleteTextChangedListener(Context context){
        this.context = context;
    }

    @Override
    public void afterTextChanged(Editable s) {
        // TODO Auto-generated method stub

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTextChanged(CharSequence userInput, int start, int before, int count) {

        try{

            // if you want to see in the logcat what the user types
          //  Log.e(TAG, "User input: " + userInput);

            AssetSearchActivity mainActivity = ((AssetSearchActivity) context);

            // update the adapater
            mainActivity.myAdapter.notifyDataSetChanged();

            // get suggestions from the database
            MyObject[] myObjs = mainActivity.db.read(userInput.toString(),mainActivity.SELECTED_ASSET_TYPE_ID);

            // update the adapter
            mainActivity.myAdapter = new AutocompleteCustomArrayAdapter(mainActivity, R.layout.list_view_row_item, myObjs);

            mainActivity.binding.myAutoComplete.setAdapter(mainActivity.myAdapter);

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



}