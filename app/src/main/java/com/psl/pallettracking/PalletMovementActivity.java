package com.psl.pallettracking;

import static com.psl.pallettracking.ext.DataExt.getCategoryID;
import static com.psl.pallettracking.ext.DataExt.getTagType;
import static com.psl.pallettracking.ext.DataExt.typeBean;
import static com.psl.pallettracking.helper.AssetUtils.hideProgressDialog;
import static com.psl.pallettracking.helper.AssetUtils.showProgress;

import static com.psl.pallettracking.ext.DataExt.typePallet;
import static com.psl.pallettracking.ext.DataExt.typeTemporaryStorage;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.GridLayoutManager;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.psl.pallettracking.adapters.WorkOrderDetailsAdapter;
import com.psl.pallettracking.bean.WorkOrderListItem;
import com.psl.pallettracking.bean.WorkOrderUploadTagBean;
import com.psl.pallettracking.database.DatabaseHandler;
import com.psl.pallettracking.databinding.ActivityPalletMovementBinding;
import com.psl.pallettracking.ext.DataExt;
import com.psl.pallettracking.helper.APIConstants;
import com.psl.pallettracking.helper.AssetUtils;
import com.psl.pallettracking.helper.LoadingUnloadingActivityHelpers;
import com.psl.pallettracking.helper.SharedPreferencesManager;
import com.psl.pallettracking.rfid.RFIDInterface;
import com.psl.pallettracking.rfid.SeuicGlobalRfidHandler;
import com.psl.pallettracking.viewHolder.OrderDetails;
import com.psl.pallettracking.bean.TagBean;
import com.seuic.uhf.EPC;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;


public class PalletMovementActivity extends AppCompatActivity {
    ActivityPalletMovementBinding binding;
    private Context context = this;


    private WorkOrderDetailsAdapter workOrderDetailsRecAdapter, workOrderDetailsDisAdapter;
    private List<WorkOrderListItem> orderDetailsList, recOrderDetailsList, disOrderDetailsList;
    private String workOrderType = "";
    private SeuicGlobalRfidHandler rfidHandler;
    boolean IS_API_CALL_IS_IN_PROGRESS = false;
    private int POLLING_TIMER = 10000;
    String SCANNED_EPC = "";
    int SCANNED_RSSI = Integer.MIN_VALUE;
    private int lastRssi = Integer.MIN_VALUE;
    private DatabaseHandler db;
    private String CURRENT_WORK_ORDER_NUMBER = "";
    private String CURRENT_WORK_ORDER_TYPE = "";
    private String CURRENT_WORK_ORDER_STATUS = "";
    private String LAST_SUCCEED_PALLET = "";
    private String PALLET_ID = "";
    private boolean isRfidReadingIsInProgress = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_pre_pallet_movement);
        binding = DataBindingUtil.setContentView(PalletMovementActivity.this, R.layout.activity_pallet_movement);
        setTitle("USER LOGIN");
        getSupportActionBar().hide();

        orderDetailsList = new ArrayList<>();
        recOrderDetailsList = new ArrayList<>();
        disOrderDetailsList = new ArrayList<>();

        db = new DatabaseHandler(context);
        //db.deleteOfflineTagMaster();
        binding.rvPallet.setLayoutManager(new GridLayoutManager(PalletMovementActivity.this, 1));
        if (orderDetailsList != null) {
            orderDetailsList.clear();
        }
        binding.rvPallet.setLayoutManager(new GridLayoutManager(PalletMovementActivity.this, 1));
        if (recOrderDetailsList != null) {
            recOrderDetailsList.clear();
        }
        binding.disPallet.setLayoutManager(new GridLayoutManager(PalletMovementActivity.this, 1));
        if (disOrderDetailsList != null) {
            disOrderDetailsList.clear();
        }

        workOrderDetailsRecAdapter = new WorkOrderDetailsAdapter(PalletMovementActivity.this, recOrderDetailsList, workOrderType);
        binding.rvPallet.setAdapter(workOrderDetailsRecAdapter);
        workOrderDetailsDisAdapter = new WorkOrderDetailsAdapter(PalletMovementActivity.this, disOrderDetailsList, workOrderType);
        binding.disPallet.setAdapter(workOrderDetailsDisAdapter);
        POLLING_TIMER = SharedPreferencesManager.getPollingTimer(PalletMovementActivity.this);
        binding.textDestination.setText("");
        binding.textPalletNo.setText("");
        initUHF();

        binding.btnPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AssetUtils.openPowerSettingDialog(context, rfidHandler);
            }
        });
        binding.btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCustomConfirmationDialog("Are you sure you want to clear data","CLEAR");
            }
        });
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCustomConfirmationDialog("Are you sure you want to go back","BACK");
            }
        });

    }

    private Handler workOrderPollingApiHandler = new Handler();
    private Runnable workOrderPollingApiRunnable;

    private void startWorkOrderPollingApiHandler() {
        try {
            workOrderPollingApiRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isOtherWorkIsInProgress) {
                        // binding.textDestination.setText("");
                        // binding.textPalletNo.setText("");
                        //new GetWorkOrderDetailsTask(LoadingUnloadingActivity.this).execute();
                        if (!isRfidReadingIsInProgress) {
                            if (db.getOfflineTagMasterCount() > 0) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        uploadWorkOrderItemToServer();
                                    }
                                }).start();
                            }
                            getWorkOrderDetailsFromServer();

                        }
                    }

                    workOrderPollingApiHandler.postDelayed(this, POLLING_TIMER);

                }
            };
        } catch (Exception ex) {
            Log.e("HANDEXC", ex.getMessage());
        }
        // Post the initial Runnable with a delay of 2 seconds first time start handler after 2 seconds
        workOrderPollingApiHandler.postDelayed(workOrderPollingApiRunnable, 2000);
    }

    private void stopWorkOrderPollingApiHandler() {
        // Remove any pending callbacks and messages
        workOrderPollingApiHandler.removeCallbacks(workOrderPollingApiRunnable);
    }

    @Override
    protected void onDestroy() {
        // Stop the handler when the activity is destroyed
        stopWorkOrderPollingApiHandler();
        if (epcs != null) {
            epcs.clear();
        }
        rfidHandler.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //getWorkOrderList();
        try {
            rfidHandler.onResume();
            startWorkOrderPollingApiHandler();
        } catch (Exception e) {
            Log.e("onresumesxc", e.getMessage());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        rfidHandler.onPause();
    }

    private void initUHF() {
        AssetUtils.showProgress(context, getResources().getString(R.string.uhf_initialization));
        rfidHandler = new SeuicGlobalRfidHandler();
        rfidHandler.onCreate(context, new RFIDInterface() {
            @Override
            public void handleTriggerPress(boolean pressed) {
                runOnUiThread(() -> {
                    if (pressed) {

                        if (orderDetailsList.size() > 0) {
                            SCANNED_EPC = "";
                            SCANNED_RSSI = Integer.MIN_VALUE;
                            startInventory();
                            new Handler().postDelayed(() -> {
                                hideProgressDialog();
                                stopInventory();
                                stopInventoryAndDoValidations();
                            }, 2000);
                        } else {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, "No Workorder list found, can not scan");
                        }
                    }
                });
            }

            @Override
            public void RFIDInitializationStatus(boolean status) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    if (status) {
                        //getWorkOrderItemDetails(workOrderNumber, workOrderType);
                    } else {
                        AssetUtils.showCommonBottomSheetErrorDialog(context, "RFID initialization failed");
                        finish();
                    }
                });
            }

            @Override
            public void handleLocateTagResponse(int value, int size) {
                runOnUiThread(() -> {

                });
            }

            @Override
            public void onDataReceived(List<EPC> rfifList) {
                runOnUiThread(() -> {
                    if (rfifList != null && rfifList.size() > 0) {
                        int maxRssi = Integer.MIN_VALUE;//changed
                        String maxRssiEpc = null;//changed
                        for (int i = 0; i < rfifList.size(); i++) {
                            String epc = rfifList.get(i).getId();
                            int rssivalue = rfifList.get(i).rssi;//changed
                            if (rssivalue > maxRssi) {
                                maxRssi = rssivalue;
                                maxRssiEpc = epc;
                            }//changed
                            try {
                                Log.e("EPC11", epc);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            if (!epcs.contains(epc)) {
                                epcs.add(epc);
                            }
                        }
                        // Show the EPC with the highest RSSI value
                        if (maxRssiEpc != null) {
                            Log.e("Max RSSI EPC", maxRssiEpc);
                            // Update your UI with the EPC with the highest RSSI value
                            SCANNED_RSSI = maxRssi;
                            SCANNED_EPC = maxRssiEpc;
                        }//changed
                    }
                });
            }
        });
    }

    public void stopInventoryAndDoValidations() {
        hideProgressDialog();
        try {
            if (SCANNED_EPC != null) {
                if (!SCANNED_EPC.isEmpty()) {
                    if (SCANNED_EPC.length() >= 24) {
                        SCANNED_EPC = SCANNED_EPC.substring(0, 24);
                        Log.e("CHILD",SCANNED_EPC);
                        if (AssetUtils.getTagType(SCANNED_EPC).equals(AssetUtils.TYPE_PALLET)
                                || AssetUtils.getTagType(SCANNED_EPC).equals(AssetUtils.TYPE_BEAN)
                                || AssetUtils.getTagType(SCANNED_EPC).equals(AssetUtils.TYPE_TEMPORARY_STORAGE)
                                || AssetUtils.getTagType(SCANNED_EPC).equals(AssetUtils.TYPE_LOADING_AREA)) {

                            TagBean tagBean = new TagBean(
                                    SCANNED_EPC,
                                    SCANNED_EPC,
                                    SCANNED_RSSI,
                                    1,
                                    "1",
                                    "",
                                    getTagType(SCANNED_EPC),
                                    AssetUtils.getSystemDateTimeInFormatt()


                            );
                            setListData(tagBean);

                        } else {
                            isRfidReadingIsInProgress = false;
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                        }
                    } else {
                        isRfidReadingIsInProgress = false;
                        AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                    }
                } else {
                    isRfidReadingIsInProgress = false;
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                }
            } else {
                isRfidReadingIsInProgress = false;
                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
            }
        } catch (Exception e) {
            Log.e("RFEXC",""+e.getMessage());
            e.printStackTrace();
            isRfidReadingIsInProgress = false;
            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.no_rfid_error));
        }
    }

    private List<String> epcs = new ArrayList<>();

    private void startInventory() {
        if (epcs != null) {
            epcs.clear();
        }
        SCANNED_EPC = "";
        showProgress(context, "Please wait...Scanning Rfid Tag");
        setFilterandStartInventory();
    }

    private void stopInventory() {
        isRfidReadingIsInProgress = false;
        rfidHandler.stopInventory();
        //adapter.notifyDataSetChanged();
    }

    private void setFilterandStartInventory() {
        int rfpower = SharedPreferencesManager.getPower(context);
        rfidHandler.setRFPower(rfpower);
        rfidHandler.startInventory();
        isRfidReadingIsInProgress = true;
    }

    private void setListData(TagBean bean1) {
        if (bean1 != null) {
            isRfidReadingIsInProgress = true;
            TagBean myBean = bean1;
            String epcId = myBean.getEpcId();
            final int rssiValue0 = myBean.getRssi();
            int rssiValue = rssiValue0;
            String antenaId = myBean.getAntenna();
            int rssi = rssiValue;
            if (rssi < 0) {
                rssi = (-1) * rssi;
                rssiValue = rssi;
            } else {

            }
            myBean.setRssi(rssi);
            if (!LoadingUnloadingActivityHelpers.isEpcPresentInWorkOrder(epcId, orderDetailsList)) {
                //Wrong Tag Found, Start Long Buzzer
                // buzzerForWrongTag();
                // return;
            }
            List<TagBean> lst = new ArrayList<>();
            lst.add(myBean);
            if (myBean.getTagType().equalsIgnoreCase(typePallet)) {
                String palletName = LoadingUnloadingActivityHelpers.getPalletNameByPalletTagId(epcId, orderDetailsList);

                CURRENT_WORK_ORDER_NUMBER = LoadingUnloadingActivityHelpers.getWorkOrderNumberByPalletTagId(epcId,orderDetailsList);
                CURRENT_WORK_ORDER_TYPE = LoadingUnloadingActivityHelpers.getWorkOrderTypeByPalletTagId(epcId,orderDetailsList);
                CURRENT_WORK_ORDER_STATUS = LoadingUnloadingActivityHelpers.getWorkOrderStatusByPalletTagId(epcId,orderDetailsList);
                String destinationName = LoadingUnloadingActivityHelpers.getTagNameByOrderType(epcId,CURRENT_WORK_ORDER_TYPE,orderDetailsList);
                binding.textPalletNo.setText(palletName);
                binding.textDestination.setText(destinationName);

                if (db.isPalletTagPresent()) {
                    if (db.isEpcPresent(epcId)) {
                        //pallet tag is present and it is old one then do nothing only update that tag data
                        db.deletePalletTag(epcId);
                        db.storeTagMaster(lst);
                    } else {
                        //Other pallet tag found, Now here need to check RSSI,and also check if other tags are scanned,,,, give rssi high preference, here other pallet tag is scanned
                        try {
                            long tagCount = db.getTagMasterCount();
                            //Replace pallet tag only if other tags are not scanned
                            if (tagCount == 1) {
                                String oldRSSIString = db.getRSSIPalletEPC();
                                int oldRSSI = Integer.parseInt(oldRSSIString);
                                int newRSSI = rssiValue;
                                if (oldRSSI < 0) {
                                    oldRSSI = (-1) * oldRSSI;
                                } else {
                                    oldRSSI = oldRSSI;
                                }

                                if (newRSSI < 0) {
                                    newRSSI = (-1) * newRSSI;
                                } else {
                                    newRSSI = newRSSI;
                                }
                                if (newRSSI <= oldRSSI) {
                                    //replace only new pallet tag with old pallet tag as he has greater RSSI and keep child tags as it is in transaction
                                    db.deletePalletTag(epcId);
                                    db.storeTagMaster(lst);
                                    int finalRssiValue2 = rssiValue;
                                 /*   new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            sendCurrentScannedPallet(epcId, finalRssiValue2);
                                        }
                                    }).start();*/
                                }
                            }
                        } catch (Exception ex) {
                            Log.e("RSSIEXC", "" + ex.getMessage());
                        }
                    }

                } else {
                    db.storeTagMaster(lst);
                }
            } else {
                if (db.isPalletTagPresent()) {
                    isRfidReadingIsInProgress = true;
                    //check here type of workorder and then get corrosponding child tags.
                    // and check if current tag is corresponding to pallet tag then do action
                    String palletTag = db.getPalletTag().getEpcId();
                    Log.e("childtag",epcId);

                    if (palletTag != null && !palletTag.equalsIgnoreCase("")) {
                        Log.e("destid-UU",CURRENT_WORK_ORDER_TYPE);
                        switch (CURRENT_WORK_ORDER_TYPE) {

                            case "":
                                isRfidReadingIsInProgress = false;
                                break;
                            case "U0":
                                String u0LoadingAreaTagId = LoadingUnloadingActivityHelpers.getU0LoadingAreaTagIdForPallet(palletTag, orderDetailsList);
                                Log.e("destid-U0",u0LoadingAreaTagId);
                                if (!epcId.equalsIgnoreCase(u0LoadingAreaTagId)) {
                                    //buzzerForWrongTag();
                                    isRfidReadingIsInProgress = false;
                                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                                    Log.e("STOPRFID", "AREA TAG NOT IN WORK ORDER U0:" + epcId);
                                    // return;

                                }
                                //add tag into db and take action.
                                addTagAndTakeAction(lst, "U0");


                                break;
                            case "U1":
//                                String u1BinLocationTagId = LoadingUnloadingActivityHelpers.getU1BinTagIdForPallet(palletTag, orderDetailsList);
//                                Log.e("destid-U1",u1BinLocationTagId);
//                                if (!epcId.equalsIgnoreCase(u1BinLocationTagId)) {
//                                    //buzzerForWrongTag();
//                                    isRfidReadingIsInProgress = false;
//                                    Log.e("STOPRFID", "BIN TAG NOT IN WORK ORDER U1:" + epcId);
//
//                                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
//
//                                }
                                if(getTagType(epcId).equalsIgnoreCase(typeBean)){
                                    addTagAndTakeAction(lst, "U1");
                                } else{
                                    isRfidReadingIsInProgress = false;
                                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                                }
                                break;
                            case "L0":
                                Log.e("destid-L0",epcId);
                                if (getTagType(epcId).equalsIgnoreCase(typeTemporaryStorage)) {
                                    addTagAndTakeAction(lst, "L0");
                                }else{
                                    isRfidReadingIsInProgress = false;
                                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                                }
                                break;
                            case "L1":
                                String l1LoadingAreaTagId = LoadingUnloadingActivityHelpers.getL1LoadingAreaTagIdForPallet(palletTag, orderDetailsList);
                                Log.e("destid-L1",l1LoadingAreaTagId);
                                if (!epcId.equalsIgnoreCase(l1LoadingAreaTagId)) {
                                    //buzzerForWrongTag();
                                    isRfidReadingIsInProgress = false;
                                    Log.e("STOPRFID", "AREA TAG NOT IN WORK ORDER L1:" + epcId);

                                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                                }

                                addTagAndTakeAction(lst, "L1");
                                break;
                        }
                        isRfidReadingIsInProgress = false;
                    }
                }else{
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                }
            }
            isRfidReadingIsInProgress = false;

        }
    }
    private void addTagAndTakeAction(List<TagBean> lst, String workOrderType) {
        db.storeTagMaster(lst);
        Log.e("action",""+lst.get(0).getEpcId());

        List<TagBean> allTags = db.getAllTagData();
        List<WorkOrderUploadTagBean> allTagsOffline1 = new ArrayList<>();
        String batchId = APIConstants.getSystemDateTimeForBatchId();
        for (int i = 0; i < allTags.size(); i++) {
            TagBean bean1 = allTags.get(i);
            bean1.setBatchId(batchId);

            String workOrderNumber = CURRENT_WORK_ORDER_NUMBER;
            workOrderType = CURRENT_WORK_ORDER_TYPE;
            WorkOrderUploadTagBean workOrderUploadTagBean = new WorkOrderUploadTagBean(bean1, workOrderNumber, workOrderType);
            Log.e("WokrUpload", workOrderUploadTagBean.getWorkOrderNumber());
            allTagsOffline1.add(workOrderUploadTagBean);
        }
        db.storeOfflineTagMaster(allTagsOffline1);

        Log.e("LocalDb", db.getAllPalletTags().toString());
        List<WorkOrderListItem> updatedAll = LoadingUnloadingActivityHelpers.getUpdatedTotalOrders(orderDetailsList, db.getPalletTag().getEpcId(), "Completed");
        List<WorkOrderListItem> updatedDis = LoadingUnloadingActivityHelpers.getUpdatedDisOrders(disOrderDetailsList, db.getPalletTag().getEpcId(), "Completed");
        List<WorkOrderListItem> updatedrec = LoadingUnloadingActivityHelpers.getUpdatedRecOrders(recOrderDetailsList, db.getPalletTag().getEpcId(), "Completed");

        db.deleteTagMaster();//changed
        if(orderDetailsList!=null){
            orderDetailsList.clear();
            orderDetailsList.addAll(updatedAll);
            Log.e("thisorderstatus","CompletedALL"+updatedAll.size());
        }
        if(workOrderType.equalsIgnoreCase("L0") || workOrderType.equalsIgnoreCase("L1")){
            if(disOrderDetailsList!=null){
                disOrderDetailsList.clear();
                binding.disCount.setText(""+disOrderDetailsList.size());
                disOrderDetailsList.addAll(updatedDis);
                Log.e("thisorderstatus","CompletedDIS"+updatedDis.size());
            }
            workOrderDetailsDisAdapter.notifyDataSetChanged();
        }

        if(workOrderType.equalsIgnoreCase("U0") || workOrderType.equalsIgnoreCase("U1")){
            if(recOrderDetailsList!=null){
                recOrderDetailsList.clear();
                binding.recCount.setText(""+recOrderDetailsList.size());
                recOrderDetailsList.addAll(updatedrec);
                Log.e("thisorderstatus","CompletedREC"+updatedrec.size());
            }
            workOrderDetailsRecAdapter.notifyDataSetChanged();
        }


        Log.e("thisorderstatus","Completed");

        binding.textPalletNo.setText("");
        binding.textDestination.setText("");

        CURRENT_WORK_ORDER_STATUS = "";
        CURRENT_WORK_ORDER_TYPE = "";
        CURRENT_WORK_ORDER_NUMBER = "";
        isRfidReadingIsInProgress = false;
//        if (db.getOfflineTagMasterCount() > 0) {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    isRfidReadingIsInProgress = true;
//                    uploadWorkOrderItemToServer();
//                }
//            }).start();
//        }
    }


    boolean isOtherWorkIsInProgress = false;

    private void getWorkOrderDetailsFromServer() {
        try {
            isOtherWorkIsInProgress = true;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(APIConstants.DEVICE_ID, SharedPreferencesManager.getDeviceId(context));
            //jsonObject.put(APIConstants.READER_STATUS, "1");

            OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                    .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                    .build();
            String Url = SharedPreferencesManager.getHostUrl(context) + APIConstants.M_GET_WORK_ORDER_DETAILS_FOR_PDA;

            AndroidNetworking.post(SharedPreferencesManager.getHostUrl(context) + APIConstants.M_GET_WORK_ORDER_DETAILS_FOR_PDA).addJSONObjectBody(jsonObject)
                    .setTag("test")
                    //.addHeaders("Authorization",SharedPreferencesManager.getAccessToken(context))
                    .setPriority(Priority.LOW)
                    .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject result) {
                            Log.e("result", result.toString());
                            isOtherWorkIsInProgress = false;
                            try {
                                if (result.getString(APIConstants.STATUS).equalsIgnoreCase("true")) {

                                    if (orderDetailsList != null) {
                                        orderDetailsList.clear();

                                    }
                                    if (recOrderDetailsList != null) {
                                        recOrderDetailsList.clear();
                                        binding.recCount.setText("0");

                                    }
                                    if (disOrderDetailsList != null) {
                                        disOrderDetailsList.clear();
                                        binding.disCount.setText("0");
                                    }

                                    boolean startOperation = true;
                                    if (result.has(APIConstants.DATA)) {
                                        try {
                                            JSONArray dataArray = result.getJSONArray(APIConstants.DATA);
                                            if (dataArray != null) {
                                                if (dataArray.length() == 0) {
                                                    startOperation = false;
                                                }
                                            } else {
                                                startOperation = false;
                                            }
                                            Log.e("result2", "result2 passed");
                                        } catch (JSONException ex) {
                                            startOperation = false;
                                            Log.e("result1", ex.getMessage());
                                        }
                                    }
                                    if (result.has(APIConstants.DATA)) {
                                        Log.e("result9", "get data array");
                                        JSONArray dataArray = result.getJSONArray(APIConstants.DATA);
                                        Log.e("result9", "got data array");
                                        if (dataArray.length() > 0) {
                                            for (int i = 0; i < dataArray.length(); i++) {
                                                JSONObject dataObject = dataArray.getJSONObject(i);
                                                Log.e("result8", String.valueOf(i));
                                                WorkOrderListItem workOrderListItem = new WorkOrderListItem();
                                                String workorderNumber = "";
                                                String workorderType = "";
                                                String workorderStatus = "";
                                                String palletName = "";
                                                String palletTagId = "";
                                                String lastUpdateDateTime = "";
                                                String tempStorageName = "";
                                                String tempStorageTagId = "";
                                                String loadingAreaName = "";
                                                String loadingAreaTagId = "";
                                                String binLocationName = "";
                                                String binLocationTagId = "";
                                                String listItemStatus = "";
                                                String destinationName = "";

                                                if (dataObject.has(APIConstants.CURRENT_WORK_ORDER_STATUS)) {
                                                    Log.e("result 3.1", "get work order status");
                                                    workorderStatus = dataObject.getString(APIConstants.CURRENT_WORK_ORDER_STATUS);
                                                    workOrderListItem.setWorkorderStatus(workorderStatus);
                                                    Log.e("result 3.2", "get work order status");
                                                    // if (workorderStatus!=null && !workorderStatus.equalsIgnoreCase(CURRENT_WORK_ORDER_STATUS)) {
                                                    if (workorderStatus != null) {
                                                        CURRENT_WORK_ORDER_STATUS = workorderStatus;
                                                        Log.e("WorkorderStatus1", workorderStatus);
                                                    } else {
                                                        Log.e("result6", "workorderstatus is null");
                                                    }
                                                } else {
                                                    Log.e("result4", "not found work order statuds");
                                                }
                                                if (dataObject.has(APIConstants.DESTINATION_LOCATION_NAME)) {

                                                    destinationName = dataObject.getString(APIConstants.DESTINATION_LOCATION_NAME);
                                                    workOrderListItem.setDestinationLocationName(destinationName);
                                                }

                                                if (dataObject.has(APIConstants.CURRENT_WORK_ORDER_NUMBER)) {

                                                    workorderNumber = dataObject.getString(APIConstants.CURRENT_WORK_ORDER_NUMBER);
                                                    workOrderListItem.setWorkorderNumber(workorderNumber);
                                                }
                                                if (dataObject.has(APIConstants.CURRENT_WORK_ORDER_TYPE)) {
                                                    workorderType = dataObject.getString(APIConstants.CURRENT_WORK_ORDER_TYPE).trim();
                                                    workOrderListItem.setWorkorderType(workorderType);
                                                }

                                                if (dataObject.has(APIConstants.CURRENT_PALLET_NAME)) {
                                                    palletName = dataObject.getString(APIConstants.CURRENT_PALLET_NAME);
                                                    workOrderListItem.setPalletName(palletName);
                                                }

                                                if (dataObject.has(APIConstants.CURRENT_PALLET_TAG_ID)) {
                                                    palletTagId = dataObject.getString(APIConstants.CURRENT_PALLET_TAG_ID);
                                                    workOrderListItem.setPalletTagId(palletTagId);
                                                    PALLET_ID = palletTagId;
                                                }

                                                if (dataObject.has(APIConstants.LAST_UPDATED_DATE_TIME)) {
                                                    lastUpdateDateTime = dataObject.getString(APIConstants.LAST_UPDATED_DATE_TIME);
                                                    workOrderListItem.setLastUpdateDateTime(lastUpdateDateTime);
                                                }

                                                if (dataObject.has(APIConstants.CURRENT_TEMP_STORAGE_NAME)) {
                                                    tempStorageName = dataObject.getString(APIConstants.CURRENT_TEMP_STORAGE_NAME);
                                                    workOrderListItem.setTempStorageName(tempStorageName);
                                                }

                                                if (dataObject.has(APIConstants.CURRENT_TEMP_STORAGE_TAG_ID)) {
                                                    tempStorageTagId = dataObject.getString(APIConstants.CURRENT_TEMP_STORAGE_TAG_ID);
                                                    workOrderListItem.setTempStorageTagId(tempStorageTagId);
                                                }

                                                if (dataObject.has(APIConstants.CURRENT_LOADING_AREA_NAME)) {
                                                    loadingAreaName = dataObject.getString(APIConstants.CURRENT_LOADING_AREA_NAME);
                                                    workOrderListItem.setLoadingAreaName(loadingAreaName);
                                                }

                                                if (dataObject.has(APIConstants.CURRENT_LOADING_AREA_TAG_ID)) {
                                                    loadingAreaTagId = dataObject.getString(APIConstants.CURRENT_LOADING_AREA_TAG_ID);
                                                    workOrderListItem.setLoadingAreaTagId(loadingAreaTagId);
                                                }

                                                if (dataObject.has(APIConstants.CURRENT_BIN_LOCATION_NAME)){
                                                    binLocationName = dataObject.getString(APIConstants.CURRENT_BIN_LOCATION_NAME);
                                                    workOrderListItem.setBinLocationName(binLocationName);
                                                }

                                                if (dataObject.has(APIConstants.CURRENT_BIN_LOCATION_TAG_ID)) {
                                                    binLocationTagId = dataObject.getString(APIConstants.CURRENT_BIN_LOCATION_TAG_ID);
                                                    workOrderListItem.setBinLocationTagId(binLocationTagId);
                                                }

                                                if (dataObject.has(APIConstants.CURRENT_WORK_ORDER_LIST_ITEM_STATUS)) {
                                                    listItemStatus = dataObject.getString(APIConstants.CURRENT_WORK_ORDER_LIST_ITEM_STATUS);
                                                    workOrderListItem.setListItemStatus(listItemStatus);
                                                }
                                                Log.e("LocalDb1", db.getAllPalletTags().toString());
                                                Log.e("LastPallet", LAST_SUCCEED_PALLET);
                                                if(palletTagId.equalsIgnoreCase(LAST_SUCCEED_PALLET)||db.isEpcPresentInOffline(palletTagId)) {
                                                    continue;
                                                }
                                                    else{
                                                    orderDetailsList.add(workOrderListItem);
                                                    LAST_SUCCEED_PALLET = "";
                                                }

                                                Log.e("TYPE",workorderType);
                                                if (workorderType.equalsIgnoreCase("U0") || workorderType.equalsIgnoreCase("U1")) {
                                                    Log.e("TYPE-U",workorderType);
                                                    if(!palletTagId.equalsIgnoreCase(LAST_SUCCEED_PALLET)||!db.isEpcPresentInOffline(palletTagId)) {
                                                        recOrderDetailsList.add(workOrderListItem);
                                                        LAST_SUCCEED_PALLET = "";
                                                    }

                                                } else if (workorderType.equalsIgnoreCase("L0") || workorderType.equalsIgnoreCase("L1")) {
                                                    Log.e("TYPE-L",workorderType);
                                                    if(!palletTagId.equalsIgnoreCase(LAST_SUCCEED_PALLET)||!db.isEpcPresentInOffline(palletTagId)) {
                                                       disOrderDetailsList.add(workOrderListItem);
                                                        LAST_SUCCEED_PALLET = "";
                                                    }
                                                }

                                            }
                                            if (workOrderDetailsRecAdapter != null) {
                                                workOrderDetailsRecAdapter.notifyDataSetChanged();
                                            }
                                            if (workOrderDetailsDisAdapter != null) {
                                                workOrderDetailsDisAdapter.notifyDataSetChanged();
                                            }
                                            Log.e("TOTALLIST",""+orderDetailsList.size());
                                            Log.e("DISLIST",""+disOrderDetailsList.size());
                                            Log.e("RECLIST",""+recOrderDetailsList.size());
                                            binding.recCount.setText(String.valueOf(recOrderDetailsList.size()));
                                            binding.disCount.setText(String.valueOf(disOrderDetailsList.size()));
                                        } else {
                                            Log.e("result7", "no data array");
                                        }
                                    }
                                    workOrderDetailsRecAdapter.notifyDataSetChanged();
                                    workOrderDetailsDisAdapter.notifyDataSetChanged();
                                } else {
                                    Log.e("STOPRFID", "SERVER RESULT FALSE FOR API");

                                    stopInventory();
                                    isRfidReadingIsInProgress = false;
                                    db.deleteTagMaster();
                                    if (orderDetailsList != null) {
                                        orderDetailsList.clear();
                                    }
                                    CURRENT_WORK_ORDER_TYPE = "";
                                    CURRENT_WORK_ORDER_NUMBER = "";
                                    CURRENT_WORK_ORDER_STATUS = "";
                                }
                                isOtherWorkIsInProgress = false;
                            } catch (JSONException e) {
                                isOtherWorkIsInProgress = false;
                                Log.e("EXC1", e.getMessage());
                                // throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onError(ANError anError) {
                            Log.e("error", anError.getErrorDetail());
                            Log.e("errorcode", "" + anError.getErrorCode());
                            isOtherWorkIsInProgress = false;
                        }
                    });
            isOtherWorkIsInProgress = false;
            Log.e("URL", "" + Url);
            Log.e("URL", jsonObject.toString());
        } catch (JSONException e) {
            Log.e("Exception", e.getMessage());
        }
    }

    private void uploadWorkOrderItemToServer() {
        try {
            String topBatchId = db.getTopBatchId();
            if (topBatchId == null) {
                isRfidReadingIsInProgress = false;
                isOtherWorkIsInProgress = false;
                return;
            }
            WorkOrderUploadTagBean bean = db.getPalletTagForBatchId(topBatchId);
            Log.e("BEFORE4", topBatchId);
            if (bean == null) {
                db.deleteOfflineTagMasterForBatch(topBatchId);
                Log.e("INWORK", "4");
                isRfidReadingIsInProgress = false;
                isOtherWorkIsInProgress = false;
                return;
            }
            String palletTagId = bean.getEpcId();
            String workOrderNumber = bean.getWorkOrderNumber();
            String workOrderType = bean.getWorkOrderType();
            String listItemStatus = "Completed";
            String palletTagRssi = "" + bean.getRssi();
            String palletTagCount = "1";
            String TransID = UUID.randomUUID().toString();
            Log.e("COUNT", palletTagCount);
            String palletTagAntenaId = "" + bean.getAntenna();
            // String date_time = AssetUtils.getUTCSystemDateTimeInFormatt();
            String date_time = "" + bean.getAddedDateTime();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(APIConstants.DEVICE_ID, SharedPreferencesManager.getDeviceId(context));
            jsonObject.put(APIConstants.TRANS_ID, TransID);
            jsonObject.put(APIConstants.CURRENT_WORK_ORDER_NUMBER, workOrderNumber);
            jsonObject.put(APIConstants.CURRENT_WORK_ORDER_LIST_ITEM_STATUS, listItemStatus);
            jsonObject.put(APIConstants.CURRENT_WORK_ORDER_TYPE, workOrderType);
            jsonObject.put(APIConstants.RSSI, palletTagRssi);
            jsonObject.put(APIConstants.TRANSACTION_DATE_TIME, date_time);
            jsonObject.put(APIConstants.COUNT, palletTagCount);
            jsonObject.put(APIConstants.PALLET_TAG_ID, palletTagId);
            jsonObject.put(APIConstants.ANTENA_ID, palletTagAntenaId);
            jsonObject.put(APIConstants.SUB_TAG_CATEGORY_ID, getCategoryID(palletTagId));

            jsonObject.put(APIConstants.TOUCH_POINT_TYPE, "T");
            JSONArray tagDetailsArray = new JSONArray();
            List<WorkOrderUploadTagBean> allTags = db.getAllTagDataForBatch(topBatchId);
            for (int i = 0; i < allTags.size(); i++) {
                JSONObject obj = new JSONObject();
                WorkOrderUploadTagBean tagBean = allTags.get(i);
                //obj.put(APIConstants.SUB_TRANS_ID,TransID);
                obj.put(APIConstants.SUB_TAG_ID, tagBean.getEpcId());
                obj.put(APIConstants.COUNT, "0");
                obj.put(APIConstants.RSSI, "" + tagBean.getRssi());
                obj.put(APIConstants.SUB_TAG_CATEGORY_ID, getCategoryID(tagBean.getEpcId()));
                obj.put(APIConstants.SUB_TAG_TYPE, "" + tagBean.getTagType());
                obj.put(APIConstants.TRANSACTION_DATE_TIME, "" + tagBean.getAddedDateTime());
                if (!tagBean.getTagType().equalsIgnoreCase(typePallet)) {
                    tagDetailsArray.put(obj);
                }
            }
            jsonObject.put(APIConstants.SUB_TAG_DETAILS, tagDetailsArray);
            //jsonObject.put(APIConstants.K_ASSET_SERIAL_NUMBER,serialnumber);
            Log.e("OFFLINEDATA", jsonObject.toString());
            postInventoryData(topBatchId, jsonObject);
        } catch (JSONException e) {
            isRfidReadingIsInProgress = false;
            isOtherWorkIsInProgress = false;
            Log.e("Exception", e.getMessage());
        }
    }

    public void postInventoryData(String batchId, final JSONObject loginRequestObject) {
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();
        String Url = SharedPreferencesManager.getHostUrl(context) + APIConstants.M_POST_INVENTORY;

        AndroidNetworking.post(SharedPreferencesManager.getHostUrl(context) + APIConstants.M_POST_INVENTORY).addJSONObjectBody(loginRequestObject)
                .setTag("test")
                //.addHeaders("Authorization",SharedPreferencesManager.getAccessToken(context))
                .setPriority(Priority.LOW)
                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject result) {
                        Log.e("result", result.toString());

                        try {
                            if (result.getString("status").equalsIgnoreCase("true")) {

                                db.deleteOfflineTagMasterForBatch(batchId);

                                LAST_SUCCEED_PALLET = loginRequestObject.getString(APIConstants.CURRENT_PALLET_TAG_ID);
                                Log.e("LastPallet1", LAST_SUCCEED_PALLET);

                              /*  List<WorkOrderListItem> updatedAll = LoadingUnloadingActivityHelpers.getUpdatedTotalOrders(orderDetailsList, batchId, "Completed");
                                List<WorkOrderListItem> updatedDis = LoadingUnloadingActivityHelpers.getUpdatedTotalOrders(disOrderDetailsList,batchId, "Completed");
                                List<WorkOrderListItem> updatedrec = LoadingUnloadingActivityHelpers.getUpdatedTotalOrders(recOrderDetailsList, batchId, "Completed");

                                db.deleteTagMaster();
                                if(orderDetailsList!=null){
                                    orderDetailsList.clear();
                                    orderDetailsList.addAll(updatedAll);
                                }
                                if(disOrderDetailsList!=null){
                                    disOrderDetailsList.clear();
                                    disOrderDetailsList.addAll(updatedDis);
                                }
                                if(recOrderDetailsList!=null){
                                    recOrderDetailsList.clear();
                                    recOrderDetailsList.addAll(updatedrec);
                                }*/

                                Iterator<WorkOrderListItem> iterator = orderDetailsList.iterator();
                                while (iterator.hasNext()) {
                                    WorkOrderListItem obj = iterator.next();
                                    if (obj.getPalletTagId().equals(loginRequestObject.getString(APIConstants.CURRENT_PALLET_TAG_ID))) {
                                        iterator.remove();
                                    }
                                }


                                Iterator<WorkOrderListItem> iterator1 = disOrderDetailsList.iterator();
                                while (iterator1.hasNext()) {
                                    WorkOrderListItem obj1 = iterator1.next();
                                    if (obj1.getPalletTagId().equals(loginRequestObject.getString(APIConstants.CURRENT_PALLET_TAG_ID))) {
                                        iterator1.remove();
                                    }
                                }

                                Iterator<WorkOrderListItem> iterator2 = recOrderDetailsList.iterator();
                                while (iterator2.hasNext()) {
                                    WorkOrderListItem obj2 = iterator2.next();
                                    if (obj2.getPalletTagId().equals(loginRequestObject.getString(APIConstants.CURRENT_PALLET_TAG_ID))) {
                                        iterator2.remove();
                                    }
                                }
                                  List<WorkOrderListItem> updatedAll = LoadingUnloadingActivityHelpers.getUpdatedTotalOrders(orderDetailsList, batchId, "Completed");
                                List<WorkOrderListItem> updatedDis = LoadingUnloadingActivityHelpers.getUpdatedTotalOrders(disOrderDetailsList,batchId, "Completed");
                                List<WorkOrderListItem> updatedrec = LoadingUnloadingActivityHelpers.getUpdatedTotalOrders(recOrderDetailsList, batchId, "Completed");

                                db.deleteTagMaster();
                                if(orderDetailsList!=null){
                                    orderDetailsList.clear();
                                    orderDetailsList.addAll(updatedAll);
                                }
                                if(workOrderType.equalsIgnoreCase("L0") || workOrderType.equalsIgnoreCase("L1")){
                                    if(disOrderDetailsList!=null){
                                        disOrderDetailsList.clear();
                                        binding.disCount.setText(""+disOrderDetailsList.size());
                                        disOrderDetailsList.addAll(updatedDis);
                                    }
                                    workOrderDetailsDisAdapter.notifyDataSetChanged();
                                }

                                if(workOrderType.equalsIgnoreCase("U0") || workOrderType.equalsIgnoreCase("U1")){
                                    if(recOrderDetailsList!=null){
                                        recOrderDetailsList.clear();
                                        binding.recCount.setText(""+recOrderDetailsList.size());
                                        recOrderDetailsList.addAll(updatedrec);
                                    }
                                    workOrderDetailsRecAdapter.notifyDataSetChanged();
                                }

                                isRfidReadingIsInProgress = false;
                                isOtherWorkIsInProgress = false;
                                AssetUtils.showCommonBottomSheetSuccessDialog(context, "The pallet has been moved successfully");
                            } else {
                                AssetUtils.showCommonBottomSheetErrorDialog(context, "Internal Server Error");
                            }
                        } catch (JSONException e) {
                            // throw new RuntimeException(e);
                            AssetUtils.showCommonBottomSheetErrorDialog(context, e.getMessage());
                            hideProgressDialog();
                            isRfidReadingIsInProgress = false;
                            isOtherWorkIsInProgress = false;
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        hideProgressDialog();
                        isRfidReadingIsInProgress = false;
                        isOtherWorkIsInProgress = false;
                        Log.e("error", anError.getErrorDetail());
                        Log.e("errorcode", "" + anError.getErrorCode());
                    }
                });
        Log.e("URL", "" + Url);
        Log.e("URL", loginRequestObject.toString());
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
                if (action.equals("CLEAR")) {
                    binding.textPalletNo.setText("");
                    binding.textDestination.setText("");
                    //db.deleteOfflineTagMaster();
                }
                else if (action.equals("BACK")) {
                    finish();
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
}