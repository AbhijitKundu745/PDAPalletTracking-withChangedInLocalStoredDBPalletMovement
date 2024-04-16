package com.psl.pallettracking;

import static com.psl.pallettracking.helper.AppConstants.UNKNOWN_ASSET;
import static com.psl.pallettracking.helper.AssetUtils.hideProgressDialog;
import static com.psl.pallettracking.helper.AssetUtils.showProgress;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.psl.pallettracking.adapters.ContainerPalletMapAdapter;
import com.psl.pallettracking.database.DatabaseHandler;
import com.psl.pallettracking.databinding.ActivityPalletContainerMappingBinding;
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
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class PalletContainerMappingActivity extends AppCompatActivity {
    private Context context = this;
    private SeuicGlobalRfidHandler rfidHandler;
    private ActivityPalletContainerMappingBinding binding;
    private ConnectionDetector cd;
    private DatabaseHandler db;
    String CONTAINER_TAG_ID = "";
    String CURRENT_EPC = "";
    String START_DATE = "";
    String END_DATE = "";
    boolean IS_CONTAINER_TAG_SCANNED = false;
    boolean IS_SCANNING_LOCKED = false;
    private boolean allow_trigger_to_press = true;
    public ArrayList<HashMap<String, String>> tagList = new ArrayList<HashMap<String, String>>();
    public ArrayList<HashMap<String, String>> palletRfidList = new ArrayList<HashMap<String, String>>();
    HashMap<String, String> hashMap = new HashMap<>();
    HashMap<String, String> palletHashMap = new HashMap<>();
    private List<String> epcs = new ArrayList<>();
    private List<String> palletRfids = new ArrayList<>();
    ContainerPalletMapAdapter adapter;
    String[] barcodeArray = {"B1", "B2", "B3", "B4", "B5", "B6", "B7", "B8", "B9", "B10"};
    String menu_id  = AppConstants.MENU_ID_CONTAINER_PALLET_MAPPING;
    String activity_type  = "";

    @Override
    public void onBackPressed() {
        if (allow_trigger_to_press) {
            showCustomConfirmationDialog(getResources().getString(R.string.confirm_cancel_scanning), "BACK");
        }else{
            stopInventory();
            showCustomConfirmationDialog(getResources().getString(R.string.confirm_cancel_scanning), "BACK");

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_pallet_container_mapping);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pallet_container_mapping);

        getSupportActionBar().hide();
        cd = new ConnectionDetector(context);
        db = new DatabaseHandler(context);
        activity_type = db.getMenuActivityNameByMenuID(menu_id);
        Log.e("TYPE",activity_type);


        adapter = new ContainerPalletMapAdapter(context, palletRfidList);
        binding.LvTags.setAdapter(adapter);

        setDefault();

        SharedPreferencesManager.setPower(context, 20);

        binding.btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tagList.size() > 0) {
                    showCustomConfirmationDialog("Are you sure you want to upload", "UPLOAD");
                }
            }
        });

        binding.btnPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allow_trigger_to_press) {
                    AssetUtils.openPowerSettingDialog(context, rfidHandler);
                }
            }
        });
        binding.btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allow_trigger_to_press) {
                    showCustomConfirmationDialog(getResources().getString(R.string.confirm_cancel_scanning), "CANCEL");
                }
            }
        });
        AssetUtils.showProgress(context, getResources().getString(R.string.uhf_initialization));
        rfidHandler = new SeuicGlobalRfidHandler();
        rfidHandler.onCreate(context, new RFIDInterface() {
            @Override
            public void handleTriggerPress(boolean pressed) {
                runOnUiThread(() -> {
                    if (pressed) {

                        if (!IS_SCANNING_LOCKED) {
                            SCANNED_EPC = "";
                            if (IS_CONTAINER_TAG_SCANNED) {
                                IS_SCANNING_LOCKED = true;

                                startInventory();
                                new Handler().postDelayed(() -> {
                                    hideProgressDialog();
                                    IS_SCANNING_LOCKED = false;
                                    allow_trigger_to_press = true;
                                    stopInventory();
                                    stopInventoryAndDoValidations();
                                }, 2000);
                                //OPEN BARCODE SCANNER
                                //startScanning();
                            } else {
                                IS_SCANNING_LOCKED = true;
                                START_DATE = AssetUtils.getSystemDateTimeInFormatt();
                                //Start Inventory
                                //startScanning();
                                startInventory();
                                new Handler().postDelayed(() -> {
                                    hideProgressDialog();
                                    IS_SCANNING_LOCKED = false;
                                    allow_trigger_to_press = true;
                                    stopInventory();
                                    stopInventoryAndDoValidations();
                                }, 2000);
                            }
                        }
                    }
                });
            }

            @Override
            public void RFIDInitializationStatus(boolean status) {
                runOnUiThread(() -> {
                    hideProgressDialog();
                    if (status) {
                        //startInventory();
                        //init barcode scanner
                    } else {

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
                    if (rfifList != null) {
                        if (rfifList.size() > 0) {
                            for (int i = 0; i < rfifList.size(); i++) {
                                String epc = rfifList.get(i).getId();
                                try {
                                    Log.e("EPC11", epc);
                                }catch (Exception ex){

                                }
                                // String tid = rfifList.get(i).getEmbeded();
                                String tid = "FFFFFFFFFFFFFFFFFFFFFFFF";

                                if (!allow_trigger_to_press) {
                                    SCANNED_EPC = epc;
                                    hashMap = new HashMap<>();
                                    hashMap.put("EPC", epc);
                                    hashMap.put("TID", tid);
                                    hashMap.put("COUNT", "1");
                                    hashMap.put("STATUS", "0");
                                    hashMap.put("MESSAGE", "");
                                    int index = checkIsExist(epc);
                                    if (index == -1) {
                                        tagList.add(hashMap);
                                        if (!epcs.contains(epc)) {
                                            epcs.add(epc);
                                        }
                                    } else {
                                        int tagCount = Integer.parseInt(tagList.get(index).get("COUNT"), 10) + 1;
                                        hashMap.put("COUNT", String.valueOf(tagCount));
                                        tagList.set(index, hashMap);
                                    }
                                }
                            }
                        }
                    }
                });
            }
        });
    }
    int CURRENT_INDEX = -1;

    public void onListItemClicked(HashMap<String, String> hashmap) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e("CLICKED11",""+hashmap.get("PALLETTAG"));
                if (allow_trigger_to_press) {
                    Log.e("CLICKED",""+hashmap.get("PALLETTAG"));
                    int index = checkIsBarcodeExist(hashmap.get("PALLETTAG"));
                    if (index == -1) {

                    } else {
                        CURRENT_INDEX = index;
                        showCustomConfirmationDialog("Are you sure you want to delete", "DELETE");

                    }
                    Toast.makeText(context, hashmap.get("MESSAGE"), Toast.LENGTH_SHORT).show();
                }
               // Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });

    }
    private String SCANNED_EPC = "";
    public void stopInventoryAndDoValidations() {
        hideProgressDialog();
        adapter.notifyDataSetChanged();
        IS_SCANNING_LOCKED = false;
        allow_trigger_to_press = true;
        if (tagList.size() == 1) {
            hideProgressDialog();
            try {
                if (SCANNED_EPC != null) {
                    if (!SCANNED_EPC.isEmpty()) {
                        if (SCANNED_EPC.length() >= 24) {
                            CURRENT_EPC = SCANNED_EPC;
                            SCANNED_EPC = "";
                            Log.e("EPC",CURRENT_EPC);
                            CURRENT_EPC = CURRENT_EPC.substring(0, 24);
                            String companycode = CURRENT_EPC.substring(0, 2);
                            companycode = AssetUtils.hexToNumber(companycode);
                            String assettpid = CURRENT_EPC.substring(2, 4);
                            String serialnumber = CURRENT_EPC.substring(4, 12);
                            if (companycode.equalsIgnoreCase(SharedPreferencesManager.getCompanyCode(context))) {
                               if(IS_CONTAINER_TAG_SCANNED){
                                   if (assettpid.equalsIgnoreCase("02")) {//||assettpid.equalsIgnoreCase("03")) {
                                       String assetname = db.getProductNameByProductTagId(CURRENT_EPC);
                                       if(!assetname.equalsIgnoreCase(UNKNOWN_ASSET)){
                                           addPallettagToList(CURRENT_EPC,db.getProductNameByProductTagId(CURRENT_EPC));
                                           IS_CONTAINER_TAG_SCANNED = true;
                                           binding.textHint.setVisibility(View.VISIBLE);
                                           binding.textCount.setVisibility(View.VISIBLE);
                                       }else{
                                           AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.scan_pallet_tag_error));
                                       }

                                   } else {
                                       changeImageStatusToRfidScan();
                                       if (assettpid.equalsIgnoreCase("03")) {
                                           AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.scan_pallet_tag_error));
                                       } else {
                                           AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                                       }
                                   }
                               }else{
                                   if (assettpid.equalsIgnoreCase("03")) {//||assettpid.equalsIgnoreCase("03")) {
                                       CONTAINER_TAG_ID = CURRENT_EPC;
                                       Log.e("CONTAINERTAG",CONTAINER_TAG_ID);
                                       binding.edtRfidNumber.setText(db.getProductNameByProductTagId(CONTAINER_TAG_ID));
                                       IS_CONTAINER_TAG_SCANNED = true;
                                       binding.textHint.setVisibility(View.VISIBLE);
                                       binding.textCount.setVisibility(View.VISIBLE);
                                   } else {
                                       changeImageStatusToRfidScan();
                                       if (assettpid.equalsIgnoreCase("02")) {
                                           AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.scan_container_tag_error));
                                       } else {
                                           AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                                       }
                                   }
                               }

                            } else {
                                changeImageStatusToRfidScan();
                                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                            }
                        } else {
                            changeImageStatusToRfidScan();
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                        }

                    } else {
                        changeImageStatusToRfidScan();
                        AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                    }
                } else {
                    changeImageStatusToRfidScan();
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));

                }
            } catch (Exception e) {
                Log.e("INEXCEPTION", "" + e.getMessage());
                changeImageStatusToRfidScan();
                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.no_rfid_error));
            }

        } else if (tagList.size() == 0) {
            changeImageStatusToRfidScan();
            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.no_rfid_error));
            //Toast.makeText(getActivity(),"Invalid RFID Tag0",Toast.LENGTH_SHORT).show();
        } else if (tagList.size() > 1) {
            changeImageStatusToRfidScan();
            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.multiple_rfid_error));
            //Toast.makeText(getActivity(),"Invalid RFID Tag0",Toast.LENGTH_SHORT).show();

        }
        END_DATE = AssetUtils.getSystemDateTimeInFormatt();
    }

    public void setDefault() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CONTAINER_TAG_ID = "";
                CURRENT_EPC = "";
                IS_SCANNING_LOCKED = false;
                changeImageStatusToRfidScan();
                binding.imgStatus.setImageDrawable(getDrawable(R.drawable.rfidscan));
                binding.edtRfidNumber.setText("");
                allow_trigger_to_press = true;
                IS_CONTAINER_TAG_SCANNED = false;
                binding.textHint.setVisibility(View.GONE);
                binding.textCount.setVisibility(View.GONE);
                if (epcs != null) {
                    epcs.clear();
                }
                if (tagList != null) {
                    tagList.clear();
                }
                if (palletRfidList != null) {
                    palletRfidList.clear();
                }
            }
        });
    }

    public void changeImageStatusToRfidScan() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideProgressDialog();
                CURRENT_EPC = "";
                //CONTAINER_TAG_ID = "";
                //binding.edtRfidNumber.setText("");
                binding.imgStatus.setImageDrawable(getDrawable(R.drawable.rfidscan));
                allow_trigger_to_press = true;
                adapter.notifyDataSetChanged();
            }
        });

    }

    private void startInventory() {
        if (allow_trigger_to_press) {
            if (epcs != null) {
                epcs.clear();
            }
            if (tagList != null) {
                tagList.clear();
            }
            adapter.notifyDataSetChanged();
            allow_trigger_to_press = false;
            CURRENT_EPC = "";
            showProgress(context, "Please wait...Scanning Rfid Tag");
            setFilterandStartInventory();
            CURRENT_EPC = "";
        } else {
            hideProgressDialog();
        }
    }

    private void stopInventory() {
        rfidHandler.stopInventory();
        allow_trigger_to_press = true;
        adapter.notifyDataSetChanged();
    }

    private void setFilterandStartInventory() {
        int rfpower = SharedPreferencesManager.getPower(context);
        rfidHandler.setRFPower(rfpower);

        rfidHandler.startInventory();
    }

    @Override
    public void onResume() {
        super.onResume();
        rfidHandler.onResume();
    }

    @Override
    public void onDestroy() {
        rfidHandler.onDestroy();
        super.onDestroy();

    }

    @Override
    public void onPause() {
        super.onPause();

        rfidHandler.onPause();
    }


    public int checkIsExist(String epc) {
        if (StringUtils.isEmpty(epc)) {
            return -1;
        }
        return binarySearch(epcs, epc);
    }

    public int checkIsBarcodeExist(String barcode) {
        if (StringUtils.isEmpty(barcode)) {
            return -1;
        }
        return binarySearch(palletRfids, barcode);
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
                    setDefault();
                }
                else if (action.equals("BACK")) {
                    setDefault();
                    finish();
                }
                else if (action.equals("DELETE")) {
                    palletRfidList.remove(CURRENT_INDEX);
                    adapter.notifyDataSetChanged();
                    CURRENT_INDEX = -1;
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

    private void uploadInventoryToServer() {

        if (palletRfidList.size() > 0) {
            new CollectInventoryData().execute("ABC");
        } else {
            allow_trigger_to_press = true;
            AssetUtils.showCommonBottomSheetErrorDialog(context, "No data to upload");
        }

    }


    public class CollectInventoryData extends AsyncTask<String, String, JSONObject> {
        protected void onPreExecute() {
            showProgress(context, "Collectiong Data To Upload");
            super.onPreExecute();
        }

        protected JSONObject doInBackground(String... params) {
            if (palletRfidList.size() > 0) {
                try {
                    JSONObject jsonobject = null;
                    jsonobject = new JSONObject();
                    jsonobject.put(APIConstants.K_CUSTOMER_ID, SharedPreferencesManager.getCustomerId(context));
                    jsonobject.put(APIConstants.K_USER_ID, SharedPreferencesManager.getSavedUserId(context));
                    jsonobject.put(APIConstants.K_DEVICE_ID, SharedPreferencesManager.getDeviceId(context));
                    jsonobject.put(APIConstants.K_CONTAINER_ID, CONTAINER_TAG_ID);

                    jsonobject.put(APIConstants.K_ACTIVITY_ID, "PalletContainer"+SharedPreferencesManager.getDeviceId(context)+AssetUtils.getSystemDateTimeInFormatt());
                    jsonobject.put(APIConstants.K_ACTIVITY_TYPE, activity_type);
                    jsonobject.put(APIConstants.K_INVENTORY_START_DATE_TIME, START_DATE);
                    jsonobject.put(APIConstants.K_INVENTORY_END_DATE_TIME, END_DATE);
                    jsonobject.put(APIConstants.K_TOUCH_POINT_ID, "1");
                    jsonobject.put(APIConstants.K_INVENTORY_COUNT, palletRfidList.size());
                    jsonobject.put(APIConstants.K_PARENT_TAG_ID, CONTAINER_TAG_ID);
                    jsonobject.put(APIConstants.K_PARENT_ASSET_TYPE, "Container");

                    JSONArray js = new JSONArray();
                    for (int i = 0; i < palletRfidList.size(); i++) {
                        //String epc = palletRfidList.get(i).get("PALLETTAG");
                        //js.put(epc);

                        JSONObject barcodeObject = new JSONObject();
                        String epc = palletRfidList.get(i).get("PALLETNAME");
                        barcodeObject.put(APIConstants.K_ACTIVITY_DETAILS_ID, epc+AssetUtils.getSystemDateTimeInFormatt());
                        barcodeObject.put(APIConstants.K_ASSET_TYPE_NAME, epc);
                        barcodeObject.put(APIConstants.K_ACTIVITY_ID, epc+AssetUtils.getSystemDateTimeInFormatt());
                        barcodeObject.put(APIConstants.K_TRANSACTION_DATE_TIME, AssetUtils.getSystemDateTimeInFormatt());

                        js.put(barcodeObject);
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
                        allow_trigger_to_press = false;
                        hideProgressDialog();
                        Log.e("REQUEST",result.toString());
                        ////uploadInventory(result, APIConstants.M_UPLOAD_CONTAINER_PALLET_MAPPING, "Please wait...\n" + " Mapping is in progress");
                        uploadInventory(result, APIConstants.M_UPLOAD_INVENTORY, "Please wait...\n" + " Mapping is in progress");

                    } catch (OutOfMemoryError e) {
                        hideProgressDialog();
                        allow_trigger_to_press = false;
                        AssetUtils.showCommonBottomSheetErrorDialog(context, "Huge Data cannot be uploaded");
                    }

                } else {
                    hideProgressDialog();
                    allow_trigger_to_press = true;
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                }
            } else {
                hideProgressDialog();
                allow_trigger_to_press  = true;;
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

        Log.e("RACKPALLETMAPURL",SharedPreferencesManager.getHostUrl(context)+METHOD_NAME);
        Log.e("RACKPALLETMAPRES",loginRequestObject.toString());
        AndroidNetworking.post(SharedPreferencesManager.getHostUrl(context) + METHOD_NAME).addJSONObjectBody(loginRequestObject)
                .setTag("test")
                .setPriority(Priority.LOW)
                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject result) {
                        hideProgressDialog();
                        allow_trigger_to_press =true;
                        if (result != null) {
                            try {
                                Log.e("RACKPALLETMAPRES",result.toString());
                                String status = result.getString(APIConstants.K_STATUS);
                                String message = result.getString(APIConstants.K_MESSAGE);

                                if (status.equalsIgnoreCase("true")) {
                                    allow_trigger_to_press  = false;
                                    //TODO do validations
                                    JSONArray data=result.getJSONArray(APIConstants.K_DATA);
                                    checkResponseAndDovalidations(data);
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
    private void checkResponseAndDovalidations(JSONArray dataArray){
        if(dataArray.length()>0){
            boolean workStatus = true;
            for(int i=0;i<dataArray.length();i++){
                try {
                    JSONObject dataObject = dataArray.getJSONObject(i);

                    String pallet_tag_id = "";
                    String pallet_name = "";
                    String container_tag_id = "";
                    String container_name = "";
                    if(dataObject.has("ParentAssetID")){
                        container_tag_id = dataObject.getString("ParentAssetID").trim();
                    }
                    if(dataObject.has("ParentAssetName")){
                        container_name = dataObject.getString("ParentAssetName").trim();
                    }
                    if(dataObject.has("ChildAssetID")){
                        pallet_tag_id = dataObject.getString("ChildAssetID").trim();
                    }
                    if(dataObject.has("ChildAssetName")){
                        pallet_name = dataObject.getString("ChildAssetName").trim();
                    }
                    String status = dataObject.getString("status").trim();
                    String message = dataObject.getString("message").trim();
                    if(status.equalsIgnoreCase("false")){
                        workStatus=false;
                    }
                    palletHashMap = new HashMap<>();
                    palletHashMap.put("PALLETTAG", pallet_tag_id);
                    palletHashMap.put("PALLETNAME", pallet_name);
                    palletHashMap.put("CONTAINERTAGID", container_tag_id);
                    palletHashMap.put("CONTAINERNAME", container_name);
                    palletHashMap.put("COUNT", "1");
                    palletHashMap.put("STATUS", status);
                    palletHashMap.put("MESSAGE", message);
                    int index = checkIsBarcodeExist(pallet_tag_id);
                    if (index == -1) {
                        palletHashMap.put("STATUS", status);
                        palletRfidList.add(palletHashMap);
                        if (!palletRfids.contains(pallet_tag_id)) {
                            palletRfids.add(pallet_tag_id);
                        }
                    } else {
                        int tagCount = Integer.parseInt(palletRfidList.get(index).get("COUNT"), 10) + 1;
                        palletHashMap.put("COUNT", String.valueOf(tagCount));
                        palletRfidList.set(index, palletHashMap);
                    }
                    adapter.notifyDataSetChanged();


                } catch (JSONException e) {
                    e.printStackTrace();
                    allow_trigger_to_press = true;
                }
            }
            if(workStatus){
                setDefault();
                AssetUtils.showCommonBottomSheetSuccessDialog(context, "Mapping Done Successfully");

            }
        }

    }

    private void addPallettagToList(String epc,String palletname) {
        palletHashMap = new HashMap<>();
        palletHashMap.put("PALLETTAG", epc);
        palletHashMap.put("PALLETNAME", palletname);
        palletHashMap.put("CONTAINERTAGID", CONTAINER_TAG_ID);
        palletHashMap.put("CONTAINERNAME", db.getProductNameByProductTagId(CONTAINER_TAG_ID));
        palletHashMap.put("COUNT", "1");
        palletHashMap.put("STATUS", "true");
        palletHashMap.put("MESSAGE", "");
        int index = checkIsBarcodeExist(epc);
        if (index == -1) {
            palletRfidList.add(palletHashMap);
            if (!palletRfids.contains(epc)) {
                palletRfids.add(epc);
            }
        } else {
            int tagCount = Integer.parseInt(palletRfidList.get(index).get("COUNT"), 10) + 1;
            palletHashMap.put("COUNT", String.valueOf(tagCount));
            palletRfidList.set(index, palletHashMap);
            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.pallet_rfid_already_scanned));
        }
        binding.textCount.setText("Count : "+palletRfidList.size());
        adapter.notifyDataSetChanged();
    }
}