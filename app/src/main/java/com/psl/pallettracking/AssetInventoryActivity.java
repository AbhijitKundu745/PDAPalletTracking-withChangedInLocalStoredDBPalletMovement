package com.psl.pallettracking;

import static com.psl.pallettracking.helper.AssetUtils.hideProgressDialog;
import static com.psl.pallettracking.helper.AssetUtils.showProgress;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.psl.pallettracking.adapters.InventoryAdapter;
import com.psl.pallettracking.adapters.InventoryDetailsAdapter;
import com.psl.pallettracking.adapters.LocationSelectionAdapter;
import com.psl.pallettracking.database.DatabaseHandler;
import com.psl.pallettracking.databinding.ActivityAssetInventoryBinding;
import com.psl.pallettracking.helper.APIConstants;
import com.psl.pallettracking.helper.AppConstants;
import com.psl.pallettracking.helper.AssetUtils;
import com.psl.pallettracking.helper.ConnectionDetector;
import com.psl.pallettracking.helper.SharedPreferencesManager;
import com.psl.pallettracking.helper.StringUtils;
import com.psl.pallettracking.rfid.RFIDInterface;
import com.psl.pallettracking.rfid.SeuicGlobalRfidHandler;
import com.seuic.uhf.EPC;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import gr.escsoft.michaelprimez.searchablespinner.interfaces.IStatusListener;
import gr.escsoft.michaelprimez.searchablespinner.interfaces.OnItemSelectedListener;
import okhttp3.OkHttpClient;

public class AssetInventoryActivity extends AppCompatActivity {

    private ActivityAssetInventoryBinding binding;
    private Context context = this;
    private SeuicGlobalRfidHandler rfidHandler;
    private boolean isInventoryOn = false;
    private boolean allow_trigger_to_press = true;

    private Timer beepTimer;
    private int valid_speed = 0;

    List<String> epcList;
    HashMap<String, String> hashMap = new HashMap<>();
    private List<String> assetNames = new ArrayList<>();
    private List<String> listForInventoryDetails = new ArrayList<>();
    private List<String> unknownAssetEpcs = new ArrayList<>();
    public ArrayList<HashMap<String, String>> tagList = new ArrayList<HashMap<String, String>>();
    private InventoryAdapter adapter;
    private InventoryDetailsAdapter detailsAdapter;
    List<String> trackEpcList = new ArrayList<>();

    private long time_taken_for_inventory = 0;
    private int type = 0;

    private DatabaseHandler db;
    private ConnectionDetector cd;

    ArrayList<String> locationList = new ArrayList<>();
    private String SELECTED_LOCATION_ITEM = "";
    private String SELECTED_LOCATION_NAME = "";
    private String SELECTED_LOCATION_ID = "";
    private String START_DATE_TIME = "";
    private String END_DATE_TIME = "";
    private int SELECTED_LOCATION_POSITION = 0;

    private LocationSelectionAdapter locationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_asset_inventory);
        db = new DatabaseHandler(context);
        cd = new ConnectionDetector(context);


        getSupportActionBar().hide();
        type = getIntent().getIntExtra("type", 0);
        //Log.e("INVENTORY","Oncreate");
        //Log.e("INVENTORYTYPE",""+type);
        setDefaultViews();
        binding.textTouchPoint.setSelected(true);

        if(type==3){
            locationList = db.getAllRoomsForSearchSpinner();
        }else if(type==4) {
            //track point check with rfid and get location
            binding.textTouchPoint.setText(getResources().getString(R.string.select_track_point));
        }else if(type==5) {
            //get rfid from security master
        }else {
            locationList = db.getAllLocationsForSearchSpinner();
        }

        notifyAdapter();

        if (type == 0) {
            setTitle("ASSET INVENTORY");
            binding.textType.setText("ASSET INVENTORY");
            binding.spLocation.setVisibility(View.GONE);
            binding.textTouchPoint.setVisibility(View.GONE);
            binding.radioGroup.setVisibility(View.GONE);
        } else if (type == 1) {
            setTitle("CHECK IN");
            binding.textType.setText("CHECK IN");
            binding.spLocation.setVisibility(View.VISIBLE);
            binding.textTouchPoint.setVisibility(View.GONE);
            binding.radioGroup.setVisibility(View.GONE);
        } else if (type == 2) {
            setTitle("CHECK OUT");
            binding.textType.setText("CHECK OUT");
            binding.spLocation.setVisibility(View.VISIBLE);
            binding.textTouchPoint.setVisibility(View.GONE);
            binding.radioGroup.setVisibility(View.GONE);
        }
        else if (type == 3) {
            setTitle("ROOM CHECK OUT");
            binding.textType.setText("ROOM CHECK OUT");
            binding.spLocation.setVisibility(View.VISIBLE);
            binding.textTouchPoint.setVisibility(View.GONE);
            binding.radioGroup.setVisibility(View.GONE);
        } else if (type == 4) {
            //Based on Room Check Out only diff is read tag and get room id
            setTitle("TRACK POINT");
            binding.textType.setText("TRACK POINT");
            binding.spLocation.setVisibility(View.GONE);
            binding.textTouchPoint.setVisibility(View.VISIBLE);
            binding.radioGroup.setVisibility(View.VISIBLE);
            binding.radioGroup.setEnabled(true);
            binding.textTouchPoint.setText(getResources().getString(R.string.select_track_point));
        }else if (type == 5) {
            setTitle("SECURITY OUT");
            binding.textType.setText("SECURITY OUT");
            binding.spLocation.setVisibility(View.GONE);
            binding.textTouchPoint.setVisibility(View.GONE);
            binding.radioGroup.setVisibility(View.GONE);
        }

        epcList = new ArrayList<>();
        SharedPreferencesManager.setPower(context,30);

        adapter = new InventoryAdapter(context, tagList);
        binding.LvTags.setAdapter(adapter);

        // binding.ll.setBackgroundColor(getResources().getColor(R.color.red4));
        beepTimer = new Timer();
        beepTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //Called each time when 1000 milliseconds (1 second) (the period parameter)
                if (isInventoryOn) {
                    disableSpinner();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            time_taken_for_inventory = time_taken_for_inventory + 1;
                            binding.textInventoryIndicator.startAnimation(AnimationUtils.loadAnimation(context, R.anim.blink_text));
                            binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
                            binding.btnStartStop.setText("Stop");
                            if (valid_speed > 0) {
                                rfidHandler.playSound();
                            }
                            valid_speed = 0;
                        }
                    });

                } else {
                    enableSpinner();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
                            binding.btnStartStop.setText("Start");
                        }
                    });
                }
            }

        }, 0, 1000);

        showProgress(context, getResources().getString(R.string.uhf_initialization));
        rfidHandler = new SeuicGlobalRfidHandler();
        rfidHandler.onCreate(context, new RFIDInterface() {
            @Override
            public void handleTriggerPress(boolean pressed) {
                runOnUiThread(() -> {
                    if (pressed) {
                        if(START_DATE_TIME.equalsIgnoreCase("")){
                            START_DATE_TIME = AssetUtils.getSystemDateTimeInFormatt();
                            START_DATE_TIME = AssetUtils.getUTCSystemDateTimeInFormatt();
                        }
                        binding.btnStartStop.performClick();
                    }
                });
            }

            @Override
            public void RFIDInitializationStatus(boolean status) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    if (status) {

                    } else {

                    }
                });
            }

            @Override
            public void handleLocateTagResponse(int value, int tagSize) {
                runOnUiThread(() -> {

                });
            }

            @Override
            public void onDataReceived(List<EPC> rfifList) {
                runOnUiThread(() -> {
                    if (rfifList != null) {
                        if (rfifList.size() > 0) {
                            for (int i = 0; i < rfifList.size(); i++) {
                                String epc = rfifList.get(i).getId();
                                if (epc != null) {
                                    if (!epc.equalsIgnoreCase("")) {
                                        if (epc.length() >= 24) {
                                            epc = epc.substring(0, 24);
                                            //Log.e("EPC",epc);
                                            if(type==4){
                                                if(epcList.size()>0){
                                                    binding.radioGroup.setEnabled(false);
                                                    binding.radioButtonInventory.setEnabled(false);
                                                    binding.radioButtonRoomCheck.setEnabled(false);
                                                }
                                                if(SELECTED_LOCATION_ITEM.equalsIgnoreCase("")){
                                                    if (!trackEpcList.contains(epc)) {
                                                        trackEpcList.add(epc);
                                                    }
                                                }else{
                                                    doDataValidations(epc);
                                                }
                                            }else{
                                                if(type==5){
                                                    if(db.isRfidPresentInLostAssetMaster(epc)){

                                                    }else{
                                                        doDataValidations(epc);
                                                    }
                                                }else{
                                                    doDataValidations(epc);
                                                }
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                });
            }
        });

        binding.btnScanTrackRfid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(allow_trigger_to_press){
                    if(type==4){
                        if(SELECTED_LOCATION_ITEM.equalsIgnoreCase("")){
                            if(trackEpcList!=null){
                                trackEpcList.clear();
                            }
                            showProgress(context, "Please wait...Scanning Track Point Rfid Tag");
                            allow_trigger_to_press = false;
                            setTidFilterandStartInventory();

                            new Handler().postDelayed(() -> {
                                hideProgressDialog();
                                allow_trigger_to_press = true;
                                stopInventoryAndDoValidations();
                                rfidHandler.stopInventory();
                            }, 2000);
                        }
                    }
                }
            }
        });

        binding.btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(allow_trigger_to_press){
                    if(type==0 || type == 5){
                        takeInventoryAction();
                    }else{
                        if(SELECTED_LOCATION_ITEM.equalsIgnoreCase("")){
                            if(type==4){
                                binding.textTouchPoint.setText(getResources().getString(R.string.select_track_point));
                                binding.btnScanTrackRfid.performClick();
                                //AssetUtils.showCommonBottomSheetErrorDialog(context,getResources().getString(R.string.select_location));
                            }else{
                                AssetUtils.showCommonBottomSheetErrorDialog(context,getResources().getString(R.string.select_location));
                            }
                        }else{

                            takeInventoryAction();
                        }
                    }
                }


            }
        });
        binding.btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDefaultViews();
            }
        });


        binding.btnPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(allow_trigger_to_press){
                    AssetUtils.openPowerSettingDialog(context,rfidHandler);
                }
            }
        });

        binding.spLocation.setStatusListener(new IStatusListener() {
            @Override
            public void spinnerIsOpening() {
                binding.spLocation.hideEdit();
            }

            @Override
            public void spinnerIsClosing() {
                // LvTags.setVisibility(View.VISIBLE);
            }
        });
        binding.spLocation.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(View view, int position, long id) {

                if(isInventoryOn){
                    // binding.spAssetType.setSelectedItem(SELECTED_ASSET_POSITION);
                }else{
                    if (position == 0) {
                        SELECTED_LOCATION_ITEM = "";
                        SELECTED_LOCATION_POSITION = 0;
                    } else {
                        SELECTED_LOCATION_POSITION = position;
                        SELECTED_LOCATION_ITEM = binding.spLocation.getSelectedItem().toString();
                        SELECTED_LOCATION_NAME = SELECTED_LOCATION_ITEM.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[0];
                        SELECTED_LOCATION_ID = SELECTED_LOCATION_ITEM.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[1];
                    }
                }

            }

            @Override
            public void onNothingSelected() {
                SELECTED_LOCATION_ITEM = "";
            }
        });

        binding.btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tagList.size()>0){
                    showCustomConfirmationDialog("Are you sure you want to upload","UPLOAD");
                }
            }
        });
    }

    private void doDataValidations(String epc){
        if (!epcList.contains(epc)) {
            // String numb = epc.substring(2,12);
            String serialnumber = epc.substring(4,12);
            String typeid = epc.substring(2, 4);
            String companycode = epc.substring(0,2);
            if(companycode.equalsIgnoreCase(SharedPreferencesManager.getCompanyCode(context))){
                // if(AssetUtils.isStringContainsOnlyNumbers(numb)){
                if(!typeid.equalsIgnoreCase("00")){
                    valid_speed++;
                    epcList.add(epc);
                    binding.textTotalScanned.setText("" + epcList.size());
                    hashMap = new HashMap<>();
                    String assetname = db.getAssetTypeNameByAssetTypeId(typeid);
                    String actualassetname = db.getAssetNameByAssetSerialNumber(serialnumber);

                    if(epc.substring(0,2).equalsIgnoreCase(SharedPreferencesManager.getCompanyCode(context))){

                    }else{
                        assetname = AppConstants.UNKNOWN_ASSET;
                    }

                    if (assetname.equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)) {
                        unknownAssetEpcs.add(epc);
                    }

                    hashMap.put(AppConstants.ASSET_TYPE_ID, typeid);
                    hashMap.put(AppConstants.ASSET_TYPE_NAME, assetname);
                    hashMap.put(AppConstants.ASSET_NAME, actualassetname);
                    hashMap.put(AppConstants.ASSET_COUNT, "1");
                    hashMap.put(AppConstants.ASSET_TAG_ID, epc);

                    // if(epc.substring(0,2).equalsIgnoreCase(db.getCompanyCode())){
                    int index = checkIsAssetExist(assetname);
                    valid_speed++;
                    if (index == -1) {
                        tagList.add(hashMap);
                        if (!assetNames.contains(assetname)){
                            assetNames.add(assetname);
                        }
                    } else {

                        try {
                            int tagCount = Integer.parseInt(tagList.get(index).get(AppConstants.ASSET_COUNT), 10) + 1;
                            hashMap.put(AppConstants.ASSET_COUNT, String.valueOf(tagCount));
                            tagList.set(index, hashMap);
                        }catch (Exception e){
                            //Log.e("EXC",""+e.getMessage());
                        }


                    }
                    adapter.notifyDataSetChanged();
                }
                // }
            }

        }
    }


    public void stopInventoryAndDoValidations() {
        hideProgressDialog();
        allow_trigger_to_press = true;

        if (trackEpcList.size() == 1) {
            hideProgressDialog();
            String locationRfid = trackEpcList.get(0);
            String roomdetails = db.getRoomDetailsByRfid(locationRfid);
            if(roomdetails.equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)){
                if(trackEpcList!=null){
                    trackEpcList.clear();
                }
                SELECTED_LOCATION_ITEM = "";
                SELECTED_LOCATION_NAME = "";
                SELECTED_LOCATION_ID = "";
                binding.textTouchPoint.setText(getResources().getString(R.string.select_track_point));
                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_track_point));

            }else{
                SELECTED_LOCATION_ITEM = roomdetails;
                SELECTED_LOCATION_NAME = SELECTED_LOCATION_ITEM.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[0];
                SELECTED_LOCATION_ID = SELECTED_LOCATION_ITEM.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[1];
                binding.textTouchPoint.setText("Touch Point:"+SELECTED_LOCATION_NAME);
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        rfidHandler.setRFPower(30);
                    }
                });

            }
        } else if (trackEpcList.size() == 0) {
            if(trackEpcList!=null){
                trackEpcList.clear();
            }
            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.no_rfid_error));
            //Toast.makeText(getActivity(),"Invalid RFID Tag0",Toast.LENGTH_SHORT).show();
        } else if (trackEpcList.size() > 1) {
            if(trackEpcList!=null){
                trackEpcList.clear();
            }
            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.multiple_rfid_error));
            //Toast.makeText(getActivity(),"Invalid RFID Tag0",Toast.LENGTH_SHORT).show();

        }
    }
    private void setTidFilterandStartInventory() {
        //int rfpower = SharedPreferencesManager.getPower(context);
        rfidHandler.setRFPower(10);
        rfidHandler.startInventory();
    }

    Dialog customConfirmationDialog;

    public void showCustomConfirmationDialog(String msg, final String action) {
        if (customConfirmationDialog != null) {
            customConfirmationDialog.dismiss();
        }
        customConfirmationDialog = new Dialog(context);
        if (customConfirmationDialog != null) {
            customConfirmationDialog.dismiss();
        }
        customConfirmationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        customConfirmationDialog.setCancelable(false);
        customConfirmationDialog.setContentView(R.layout.custom_alert_dialog_layout2);
        TextView text = (TextView) customConfirmationDialog.findViewById(R.id.text_dialog);
        text.setText(msg);
        Button dialogButton = (Button) customConfirmationDialog.findViewById(R.id.btn_dialog);
        Button dialogButtonCancel = (Button) customConfirmationDialog.findViewById(R.id.btn_dialog_cancel);
        dialogButton.setText("YES");
        dialogButtonCancel.setText("NO");
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customConfirmationDialog.dismiss();
                if (action.equals("UPLOAD")) {
                    allow_trigger_to_press = false;
                    uploadInventoryToServer();
                } else if (action.equals("CANCEL")) {
                    cancelInventory();
                }
            }
        });
        dialogButtonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customConfirmationDialog.dismiss();
            }
        });
       // customConfirmationDialog.getWindow().getAttributes().windowAnimations = R.style.SlideBottomUpAnimation;
        customConfirmationDialog.show();
    }
    private void takeInventoryAction(){
        if(binding.llInitial.getVisibility()==View.VISIBLE){
            allow_trigger_to_press = true;
            if (isInventoryOn) {
                isInventoryOn = false;
                rfidHandler.stopInventory();
                //binding.ll.setBackgroundColor(getResources().getColor(R.color.red4));
                binding.textInventoryIndicator.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
                binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_red));
                binding.btnStartStop.setText("Stop");
            } else {
                isInventoryOn = true;
                rfidHandler.startInventory();
                //binding.ll.setBackgroundColor(getResources().getColor(R.color.green));
                binding.textInventoryIndicator.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
                binding.btnStartStop.setBackgroundDrawable(getResources().getDrawable(R.drawable.round_button_green));
                binding.btnStartStop.setText("Start");
            }
        }else{
            allow_trigger_to_press = false;
        }
    }
    private void notifyAdapter() {
        enableSpinner();
        binding.spLocation.setEnabled(false);
        locationAdapter = new LocationSelectionAdapter(context, locationList);
        binding.spLocation.setAdapter(locationAdapter);
        binding.spLocation.setEnabled(true);

    }

    public void enableSpinner(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.spLocation.setEnabled(true);
                binding.spLocation.setClickable(true);
            }
        });
    }

    public void disableSpinner(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.spLocation.setEnabled(false);
                binding.spLocation.setClickable(false);
                binding.spLocation.hideEdit();
            }
        });
    }
    public void setDefaultViews(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.llInitial.setVisibility(View.VISIBLE);
                binding.llDetails.setVisibility(View.GONE);
                allow_trigger_to_press = true;
            }
        });

    }
    public int checkIsAssetExist(String assetname) {
        if (StringUtils.isEmpty(assetname)) {
            return -1;
        }
        return binarySearch(assetNames, assetname);
    }

    /**
     * 二分查找，找到该值在数组中的下标，否则为-1
     */
    static int binarySearch(List<String> array, String src) {
        int left = 0;
        int right = array.size() - 1;
        // 这里必须是 <=
        while (left <= right) {
            if (compareString(array.get(left), src)) {
                return left;
            } else if (left != right) {
                if (compareString(array.get(right), src))
                    return right;
            }
            left++;
            right--;
        }
        return -1;
    }

    static boolean compareString(String str1, String str2) {
        if (str1.length() != str2.length()) {
            return false;
        } else if (str1.hashCode() != str2.hashCode()) {
            return false;
        } else {
            char[] value1 = str1.toCharArray();
            char[] value2 = str2.toCharArray();
            int size = value1.length;
            for (int k = 0; k < size; k++) {
                if (value1[k] != value2[k]) {
                    return false;
                }
            }
            return true;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        rfidHandler.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (beepTimer != null) {
            beepTimer.cancel();
        }
        if(epcList!=null){
            epcList.clear();
        }
        rfidHandler.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (beepTimer != null) {
            beepTimer.cancel();
        }
        rfidHandler.onPause();
    }

    public void onListItemClicked(String name){
       if(isInventoryOn){

       }else{

           if(listForInventoryDetails!=null){
               listForInventoryDetails.clear();
           }
           if(name.equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)){
               listForInventoryDetails.addAll(unknownAssetEpcs);

               if(listForInventoryDetails.size()>0){
                   //detailsAdapter.notifyDataSetChanged();
                   binding.llDetails.setVisibility(View.VISIBLE);
                   binding.llInitial.setVisibility(View.GONE);
               }else{

               }
           }else{

               String codeandassetid = SharedPreferencesManager.getCompanyCode(context)+db.getAssetTypeIDByAssetTypeName(name);
               for(int i=0;i<epcList.size();i++){
                   if(epcList.get(i).substring(0,4).equalsIgnoreCase(codeandassetid)){
                       listForInventoryDetails.add(epcList.get(i));
                   }
               }
               if(listForInventoryDetails.size()>0){
                   //detailsAdapter.notifyDataSetChanged();
                   binding.llDetails.setVisibility(View.VISIBLE);
                   binding.llInitial.setVisibility(View.GONE);
               }
           }

           detailsAdapter = new InventoryDetailsAdapter(context, listForInventoryDetails,name);
           binding.listDetails.setAdapter(detailsAdapter);

           binding.textSelectedAssetName.setText("ASSET TYPE: "+name+" - "+listForInventoryDetails.size());
           binding.textSelectedAssetName.setSelected(true);
           detailsAdapter.notifyDataSetChanged();
       }
    }


    public void onItemRemovedClicked(String name,String epc){

        if(name.equalsIgnoreCase(AppConstants.UNKNOWN_ASSET)){

            int index = checkIsAssetExist(name);

            if (index == -1) {

            } else {
                unknownAssetEpcs.remove(epc);
                listForInventoryDetails.remove(epc);
                epcList.remove(epc);


                HashMap<String, String> hashMap = tagList.get(index);

                int tagCount = Integer.parseInt(tagList.get(index).get(AppConstants.ASSET_COUNT), 10) - 1;
                hashMap.put(AppConstants.ASSET_COUNT, String.valueOf(tagCount));
                if(tagCount==0){
                    assetNames.remove(name);
                    tagList.remove(hashMap);
                    //tagList.set(index,hashMap);
                }else{
                    tagList.set(index, hashMap);
                }
                //tagList.set(index, hashMap);
            }
            adapter.notifyDataSetChanged();
            detailsAdapter = new InventoryDetailsAdapter(context, listForInventoryDetails,name);
            binding.listDetails.setAdapter(detailsAdapter);

            binding.textSelectedAssetName.setText("ASSET TYPE: "+name+" - "+listForInventoryDetails.size());
            binding.textSelectedAssetName.setSelected(true);
            detailsAdapter.notifyDataSetChanged();

        }else{
            int index = checkIsAssetExist(name);

            if (index == -1) {

            } else {
                listForInventoryDetails.remove(epc);
                epcList.remove(epc);

                HashMap<String, String> hashMap = tagList.get(index);
                int tagCount = Integer.parseInt(tagList.get(index).get(AppConstants.ASSET_COUNT), 10) - 1;
                hashMap.put(AppConstants.ASSET_COUNT, String.valueOf(tagCount));
                if(tagCount==0){
                    //tagList.remove(hashMap);
                    assetNames.remove(name);
                    tagList.remove(hashMap);
                   // tagList.set(index, hashMap);
                }else{
                    tagList.set(index, hashMap);
                }
            }
            adapter.notifyDataSetChanged();
            detailsAdapter = new InventoryDetailsAdapter(context, listForInventoryDetails,name);
            binding.listDetails.setAdapter(detailsAdapter);

            binding.textSelectedAssetName.setText("ASSET TYPE : "+name+" - "+listForInventoryDetails.size());
            binding.textSelectedAssetName.setSelected(true);
            detailsAdapter.notifyDataSetChanged();
        }
        binding.textTotalScanned.setText("" + epcList.size());

    }
    @Override
    public void onBackPressed() {
        rfidHandler.stopInventory();
        if (epcList.size() > 0) {
            if(type==0){
                showCustomConfirmationDialog(getResources().getString(R.string.confirm_cancel), "CANCEL");
            }
            if(type==1){
                showCustomConfirmationDialog(getResources().getString(R.string.confirm_cancel_check_in), "CANCEL");
            }
            if(type==2){
                showCustomConfirmationDialog(getResources().getString(R.string.confirm_cancel_check_out), "CANCEL");
            }
            if(type==3){
                showCustomConfirmationDialog(getResources().getString(R.string.confirm_cancel_room_check_out), "CANCEL");
            }
            if(type==4){
                showCustomConfirmationDialog(getResources().getString(R.string.confirm_cancel_track_point), "CANCEL");
            }
            if(type==5){
                showCustomConfirmationDialog(getResources().getString(R.string.confirm_cancel_security_out), "CANCEL");
            }
        } else {
            super.onBackPressed();
        }
    }

    /**
     * collect inventory data and upload to server
     */
    private void uploadInventoryToServer() {

        if (epcList.size() > 0) {
            new CollectInventoryData().execute("ABC");
        } else {
            AssetUtils.showCommonBottomSheetErrorDialog(context, "No data to upload");
        }

    }


    public class CollectInventoryData extends AsyncTask<String, String, JSONObject> {
        protected void onPreExecute() {
            showProgress(context, "Collectiong Data To Upload");
            super.onPreExecute();
        }

        protected JSONObject doInBackground(String... params) {
            if (epcList.size() > 0) {
                try {
                    JSONObject jsonobject = null;
                    jsonobject = new JSONObject();
                    jsonobject.put(APIConstants.K_CUSTOMER_ID, SharedPreferencesManager.getCustomerId(context));


                    if(type==3){
                        jsonobject.put(APIConstants.K_ROOM_ID, SELECTED_LOCATION_ID);
                        jsonobject.put(APIConstants.K_VENDOR_ID, "");
                        jsonobject.put(APIConstants.K_INVENTORY_TYPE, AssetUtils.getInventoryType(type));
                    }
                    if(type==0 || type==1 || type ==2){
                        jsonobject.put(APIConstants.K_ROOM_ID, "");
                        jsonobject.put(APIConstants.K_VENDOR_ID, SELECTED_LOCATION_ID);
                        jsonobject.put(APIConstants.K_INVENTORY_TYPE, AssetUtils.getInventoryType(type));
                    }

                    if(type==4){
                        jsonobject.put(APIConstants.K_ROOM_ID, SELECTED_LOCATION_ID);
                        jsonobject.put(APIConstants.K_VENDOR_ID, "");

                        int selectedId=binding.radioGroup.getCheckedRadioButtonId();
                        RadioButton radioTypeButton=(RadioButton)findViewById(selectedId);
                        String typestring = radioTypeButton.getText().toString().trim();
                        if(typestring.equalsIgnoreCase("Room Check")){
                            jsonobject.put(APIConstants.K_INVENTORY_TYPE, AssetUtils.getInventoryType(3));//RCO
                        }
                        if(typestring.equalsIgnoreCase("Inventory")){
                            jsonobject.put(APIConstants.K_INVENTORY_TYPE, AssetUtils.getInventoryType(type));//Track
                        }
                    }
                    if(type==5){
                        jsonobject.put(APIConstants.K_ROOM_ID, "");
                        jsonobject.put(APIConstants.K_VENDOR_ID, "");
                        jsonobject.put(APIConstants.K_INVENTORY_TYPE, AssetUtils.getInventoryType(type));

                    }
                    jsonobject.put(APIConstants.K_TOUCH_POINT_ID, "");

                    jsonobject.put(APIConstants.K_UID, SharedPreferencesManager.getSavedUserId(context));
                    jsonobject.put(APIConstants.K_INVENTORY_COUNT, ""+epcList.size());
                    jsonobject.put(APIConstants.K_DEVICE_ID, SharedPreferencesManager.getDeviceId(context));
                    jsonobject.put(APIConstants.K_INVENTORY_START_DATE_TIME, START_DATE_TIME);
                    //jsonobject.put(APIConstants.K_INVENTORY_END_DATE_TIME, AssetUtils.getSystemDateTimeInFormatt());
                    jsonobject.put(APIConstants.K_INVENTORY_END_DATE_TIME, AssetUtils.getUTCSystemDateTimeInFormatt());
                    jsonobject.put(APIConstants.K_INVENTORY_TIME,""+time_taken_for_inventory);
                    JSONArray js = new JSONArray();
                    for (int i = 0; i < epcList.size(); i++) {
                        String epc = epcList.get(i);
                        js.put(epc);
                    }
                    jsonobject.put(APIConstants.K_DATA, js);

                    return jsonobject;

                } catch (JSONException e) {

                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);

            if (result != null) {
                if (cd.isConnectingToInternet()) {
                    try {
                        hideProgressDialog();
                        if(type==0){
                            uploadInventory(result, APIConstants.M_UPLOAD_INVENTORY, "Please wait...\n" + "Inventory Data upload is in progress");
                        }
                        if(type==1){
                            uploadInventory(result, APIConstants.M_UPLOAD_INVENTORY, "Please wait...\n" + "Check IN Data upload is in progress");
                        }
                        if(type==2){
                            uploadInventory(result, APIConstants.M_UPLOAD_INVENTORY, "Please wait...\n" + "Check OUT Data upload is in progress");
                        }
                        if(type==3){
                            uploadInventory(result, APIConstants.M_UPLOAD_INVENTORY, "Please wait...\n" + "Room Check OUT Data upload is in progress");
                        }
                        if(type==4){
                            uploadInventory(result, APIConstants.M_UPLOAD_INVENTORY, "Please wait...\n" + "Track Point Data upload is in progress");
                        }
                        if(type==5){
                            uploadInventory(result, APIConstants.M_UPLOAD_INVENTORY, "Please wait...\n" + "Security Out Data upload is in progress");
                        }
                    } catch (OutOfMemoryError e) {
                        hideProgressDialog();
                        allow_trigger_to_press = true;
                        AssetUtils.showCommonBottomSheetErrorDialog(context, "Huge Data cannot be uploaded");
                    }

                } else {
                    hideProgressDialog();
                    allow_trigger_to_press = true;
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                }
            } else {
                hideProgressDialog();
                allow_trigger_to_press = true;
                AssetUtils.showCommonBottomSheetErrorDialog(context, "Something went wrong");
            }

        }

    }

    public void uploadInventory(final JSONObject loginRequestObject, String METHOD_NAME, String progress_message) {
        showProgress(context, progress_message);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();

        //Log.e("UPLOADURL",SharedPreferencesManager.getHostUrl(context)+METHOD_NAME);
        //Log.e("UPLOADREQUEST",loginRequestObject.toString());
        AndroidNetworking.post(SharedPreferencesManager.getHostUrl(context) + METHOD_NAME).addJSONObjectBody(loginRequestObject)
                .setTag("test")
                .setPriority(Priority.LOW)
                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject result) {
                        hideProgressDialog();
                        if (result != null) {
                            try {
                                //Log.e("UPLOADRESULT",result.toString());
                                String status = result.getString(APIConstants.K_STATUS);
                                String message = result.getString(APIConstants.K_MESSAGE);

                                if (status.equalsIgnoreCase("true")) {
                                    if(epcList!=null){
                                        epcList.clear();
                                    }
                                    if(tagList!=null){
                                        tagList.clear();
                                    }
                                    allow_trigger_to_press = true;

                                    if(assetNames!=null){
                                        assetNames.clear();
                                    }

                                    if(listForInventoryDetails!=null){
                                        listForInventoryDetails.clear();
                                    }
                                    if(unknownAssetEpcs!=null){
                                        unknownAssetEpcs.clear();
                                    }
                                    SELECTED_LOCATION_ITEM = "";
                                    SELECTED_LOCATION_ID = "";
                                    binding.textTouchPoint.setText(getResources().getString(R.string.select_track_point));
                                    binding.spLocation.setSelectedItem(0);
                                    AssetUtils.showCommonBottomSheetSuccessDialog(context,"Data Uploaded Successfully");
                                    adapter.notifyDataSetChanged();
                                    START_DATE_TIME = "";
                                    binding.textTotalScanned.setText("" + epcList.size());
                                    binding.radioGroup.setEnabled(true);
                                    binding.radioButtonInventory.setEnabled(true);
                                    binding.radioButtonRoomCheck.setEnabled(true);
                                } else {
                                    allow_trigger_to_press = true;
                                    AssetUtils.showCommonBottomSheetErrorDialog(context, message);
                                }
                            } catch (JSONException e) {
                                hideProgressDialog();
                                allow_trigger_to_press = true;
                                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.something_went_wrong_error));
                            }
                        } else {
                            hideProgressDialog();
                            allow_trigger_to_press = true;
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        hideProgressDialog();
                        allow_trigger_to_press = true;
                        //Log.e("ERROR", anError.getErrorDetail());
                        if (anError.getErrorDetail().equalsIgnoreCase("responseFromServerError")) {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
                        } else if (anError.getErrorDetail().equalsIgnoreCase("connectionError")) {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                        } else {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                        }
                    }
                });
    }
    /**
     * Cancel action and go back
     */
    public void cancelInventory() {
        if(epcList!=null){
            epcList.clear();
        }
        if(tagList!=null){
            tagList.clear();
        }
        allow_trigger_to_press = true;

        if(assetNames!=null){
            assetNames.clear();
        }

        if(listForInventoryDetails!=null){
            listForInventoryDetails.clear();
        }
        if(unknownAssetEpcs!=null){
            unknownAssetEpcs.clear();
        }
        SELECTED_LOCATION_ITEM = "";
       // binding.spLocation.setSelectedItem(0);
        finish();
    }


}