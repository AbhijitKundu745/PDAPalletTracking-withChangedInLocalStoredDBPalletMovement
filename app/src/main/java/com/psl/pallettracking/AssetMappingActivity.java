package com.psl.pallettracking;

import static com.psl.pallettracking.helper.AssetUtils.hideProgressDialog;
import static com.psl.pallettracking.helper.AssetUtils.showProgress;
import static com.psl.pallettracking.helper.BaseUtil.getHexByteArray;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.View;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.psl.pallettracking.adapters.AssetMappingSearchAdapter;
import com.psl.pallettracking.database.DatabaseHandler;
import com.psl.pallettracking.databinding.ActivityAssetMappingBinding;
import com.psl.pallettracking.helper.APIConstants;
import com.psl.pallettracking.helper.AppConstants;
import com.psl.pallettracking.helper.AssetUtils;
import com.psl.pallettracking.helper.BaseUtil;
import com.psl.pallettracking.helper.BeepClass;
import com.psl.pallettracking.helper.ConnectionDetector;
import com.psl.pallettracking.helper.SharedPreferencesManager;
import com.psl.pallettracking.helper.StringUtils;
import com.psl.pallettracking.rfid.RFIDInterface;
import com.psl.pallettracking.rfid.SeuicGlobalRfidHandler;
import com.seuic.uhf.EPC;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import gr.escsoft.michaelprimez.searchablespinner.interfaces.IStatusListener;
import gr.escsoft.michaelprimez.searchablespinner.interfaces.OnItemSelectedListener;
import okhttp3.OkHttpClient;

public class AssetMappingActivity extends AppCompatActivity {

    private Context context = this;
    private SeuicGlobalRfidHandler rfidHandler;
    private ActivityAssetMappingBinding binding;
    private DatabaseHandler db;
    private ConnectionDetector cd;
    byte[] currentbuteepc;

    ArrayList<String> eList = new ArrayList<>();
    AssetMappingSearchAdapter assetMappingSearchAdapter;
    private String SELECTED_ASSET = "";
    private String CURRENT_EPC = "";
    private String NEW_EPC = "";
    private String CURRENT_TID = "";
    private boolean allow_trigger_to_press = false;

    public ArrayList<HashMap<String, String>> tagList = new ArrayList<HashMap<String, String>>();
    HashMap<String, String> hashMap = new HashMap<>();
    private List<String> epcs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_asset_mapping);
        //setContentView(R.layout.activity_asset_mapping);
        //setRFPower
        getSupportActionBar().hide();
        db = new DatabaseHandler(context);
        cd = new ConnectionDetector(context);
        if (eList != null) {
            eList.clear();
        }

        SharedPreferencesManager.setPower(context,10);
        eList = db.getAllNonRegisteredAssetList();
        notifyAdapter();
        setDefault();

        AssetUtils.showProgress(context, getResources().getString(R.string.uhf_initialization));
        rfidHandler = new SeuicGlobalRfidHandler();
        rfidHandler.onCreate(context, new RFIDInterface() {
            @Override
            public void handleTriggerPress(boolean pressed) {
                runOnUiThread(() -> {
                    if (pressed) {
                        binding.btnInventory.performClick();
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
                               // String tid = rfifList.get(i).getEmbeded();
                                String tid = "FFFFFFFFFFFFFFFFFFFFFFFF";

                                if (!allow_trigger_to_press) {
                                    CURRENT_EPC = epc;
                                    CURRENT_TID = tid;
                                    currentbuteepc = rfifList.get(i).id;
                                    if (epc != null) {
                                        hashMap = new HashMap<>();
                                        hashMap.put("EPC", epc);
                                        hashMap.put("TID", tid);
                                        hashMap.put("COUNT", "1");
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
                    }
                });
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

        binding.btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SELECTED_ASSET.equalsIgnoreCase("")) {
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.select_asset));
                } else if (CURRENT_EPC.equalsIgnoreCase("")) {
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.scan_rfid_tag));
                } else if(binding.edtRfidNumber.getText().toString().equalsIgnoreCase("")){
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.scan_rfid_tag));
                }else {
                    //TODO call here api and send data to server
                    String assetname = SELECTED_ASSET.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[0];
                    String assetid = SELECTED_ASSET.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[1];
                    String assettypeid = SELECTED_ASSET.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[2];
                    String assettypename = db.getAssetTypeNameByAssetTypeId(SELECTED_ASSET.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[2]);
                    //success call here API
                    if (cd.isConnectingToInternet()) {
                        // String url = edtUrl.getText().toString().trim();
                        try {
                            hideProgressDialog();
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(APIConstants.K_ASSET_ID,assetid);
                            jsonObject.put(APIConstants.K_UID, SharedPreferencesManager.getSavedUserId(context));
                            jsonObject.put(APIConstants.K_ASSET_SERIAL_NUMBER,binding.edtRfidNumber.getText().toString().trim());
                            jsonObject.put(APIConstants.K_CUSTOMER_ID,SharedPreferencesManager.getCustomerId(context));
                            jsonObject.put(APIConstants.K_CURRENT_TAG_ID,NEW_EPC);
                            jsonObject.put(APIConstants.K_PREVIOUS_TAG_ID,CURRENT_EPC);
                            jsonObject.put(APIConstants.K_TAG_TID,CURRENT_TID);
                            doRegistration(jsonObject, APIConstants.M_ASSET_REGISTRATION, "Please wait...\n" + "Asset-RFID mapping is in progress",assetid,binding.edtRfidNumber.getText().toString());
                        } catch (JSONException e) {
                            hideProgressDialog();
                            allow_trigger_to_press = true;
                        }
                    } else {
                        hideProgressDialog();
                        allow_trigger_to_press = true;
                        AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                    }
                }
            }
        });



        binding.spAsset.setStatusListener(new IStatusListener() {
            @Override
            public void spinnerIsOpening() {
                binding.spAsset.hideEdit();
            }

            @Override
            public void spinnerIsClosing() {
                // LvTags.setVisibility(View.VISIBLE);
            }
        });

        binding.spAsset.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(View view, int position, long id) {
                if (position == 0) {
                    SELECTED_ASSET = "";
                    binding.edtAssetType.setText("");
                } else {
                    SELECTED_ASSET = binding.spAsset.getSelectedItem().toString();
                    binding.imgStatus.setImageDrawable(getDrawable(R.drawable.rfidscan));

                    CURRENT_TID = "";
                    CURRENT_EPC = "";
                    binding.edtRfidNumber.setText("");

                    String assetname = SELECTED_ASSET.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[0];
                    String assetid = SELECTED_ASSET.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[1];
                    String assettypename = db.getAssetTypeNameByAssetTypeId(SELECTED_ASSET.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[2]);
                    binding.edtAssetType.setText(assettypename);
                }
            }
            @Override
            public void onNothingSelected() {
                SELECTED_ASSET = "";
                binding.edtAssetType.setText("");
            }
        });
        binding.edtAssetType.setInputType(InputType.TYPE_NULL);
        binding.edtAssetType.setOnClickListener(v -> {
            if (SELECTED_ASSET.equalsIgnoreCase("")) {
                //select tonner equipment number
            } else {
                //openDatePickerDialog();
            }
        });

        binding.btnInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SELECTED_ASSET.equalsIgnoreCase("") || SELECTED_ASSET.equalsIgnoreCase("No Data Found") || SELECTED_ASSET.equalsIgnoreCase("Select Asset")) {
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.select_asset));
                } else {
                    if (binding.edtRfidNumber.getText().toString().equalsIgnoreCase("")) {
                        if (allow_trigger_to_press) {
                            CURRENT_EPC = "";
                            CURRENT_TID = "";
                            showProgress(context, "Please wait...Scanning Rfid Tag");
                            if (tagList != null) {
                                tagList.clear();
                            }

                            if (epcs != null) {
                                epcs.clear();
                            }
                            allow_trigger_to_press = false;
                            setTidFilterandStartInventory();

                            new Handler().postDelayed(() -> {
                                hideProgressDialog();
                                allow_trigger_to_press = true;
                                stopInventoryAndDoValidations();
                                binding.btnStopInventory.performClick();
                            }, 2000);

                        }
                    }
                }
            }
        });

        binding.btnStopInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideProgressDialog();
                rfidHandler.stopInventory();
                allow_trigger_to_press = true;
            }
        });
        binding.btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDefault();
            }
        });
    }

    private void setTidFilterandStartInventory() {
        /*byte[] embd = new byte[255];
        embd[0] = (byte) 2;//TID embeded data
        embd[1] = (byte) Integer.parseInt("0");
        embd[2] = (byte) Integer.parseInt("12");
        System.arraycopy(getHexByteArray("00000000"), 0, embd, 3, 4);*/
        //rfidHandler.mDevice.setParamBytes(UHFService.PARAMETER_TAG_EMBEDEDDATA, embd);
        // rfidHandler.mDevice.setParamBytes(UHFService.PARAMETER_TAG_EMBEDEDDATA, null);
        int rfpower = SharedPreferencesManager.getPower(context);
        rfidHandler.setRFPower(rfpower);

        rfidHandler.startInventory();
    }

    public static final int MAX_LEN = 64;

    int writecount = 0;
    public void stopInventoryAndDoValidations() {
        writecount = 0;
        hideProgressDialog();
        allow_trigger_to_press = true;
        if (tagList.size() == 1) {
            hideProgressDialog();
            CURRENT_EPC = tagList.get(0).get("EPC");
            CURRENT_TID = tagList.get(0).get("TID");
            CURRENT_TID = "FFFFFFFFFFFFFFFFFFFFFFFF";

            //AssetUtils.showCustomErrorDialog(context,"EPC: "+CURRENT_EPC+"\n"+"TID : "+CURRENT_TID);
            try {
                if (CURRENT_EPC != null) {
                    if (!CURRENT_EPC.isEmpty()) {
                        if (!CURRENT_TID.isEmpty()) {

                            if (CURRENT_EPC.length() >= 24) {
                                if (CURRENT_TID.length() >= 24) {
                                    CURRENT_EPC = CURRENT_EPC.substring(0, 24);
                                    CURRENT_TID = CURRENT_TID.substring(0, 24);
                                    String companycode = CURRENT_EPC.substring(0,2);
                                    String assettpid = CURRENT_EPC.substring(2,4);
                                    String serialnumber = CURRENT_EPC.substring(4,12);

                                    if (companycode.equalsIgnoreCase(SharedPreferencesManager.getCompanyCode(context))) {
                                       // String assetidandserialnumber = CURRENT_EPC.substring(2, 12);
                                        //if (AssetUtils.isStringContainsOnlyNumbers(assetidandserialnumber)) {
                                        if(db.isSerailNumberAlreadyRegistered(serialnumber)){
                                            changeImageStatusToRfidScan();
                                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.rfid_already_registered));
                                        }else{
                                            if(db.isSerailNumberAlreadyRegisteredToThisTypeId(serialnumber,assettpid)){
                                                changeImageStatusToRfidScan();
                                                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.rfid_already_registered));
                                            }
                                            else{
                                                binding.edtRfidNumber.setText(CURRENT_EPC.substring(4, 12));
                                                binding.imgStatus.setImageDrawable(getDrawable(R.drawable.success));
                                                if (SELECTED_ASSET.equalsIgnoreCase("")) {
                                                    changeImageStatusToRfidScan();
                                                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.rfid_already_registered));
                                                } else {
                                                    allow_trigger_to_press = false;
                                                    // new writeEpcAsync().onPostExecute("");
                                                    writeTagData(serialnumber);
                                                }
                                            }
                                        }

                                       /* } else {
                                            changeImageStatusToRfidScan();
                                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.invalid_rfid_error));
                                        }*/
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
    }

    private void writeTagData(String serialnumber) {
        writecount++;
        String assettypeid = SELECTED_ASSET.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[2];
        String assetid = SELECTED_ASSET.split(AppConstants.ASSET_TYPE_SPLIT_DATA)[1];
        int bank = 1;
        int address = 5;
        int length = 1;
        String str_password = SharedPreferencesManager.getCurrentAccessPassword(context);
        Log.e("PASSWORD",str_password);
        byte[] btPassword = new byte[16];
        BaseUtil.getHexByteArray(str_password, btPassword, btPassword.length);

        String str_data = assettypeid.toString().replace(" ", "");
        byte[] buffer = new byte[MAX_LEN];

        if (length > MAX_LEN) {
            buffer = new byte[length];
        }
        BaseUtil.getHexByteArray(str_data, buffer, buffer.length);
        byte[] finalBuffer = buffer;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean isdatawritten = rfidHandler.mDevice.writeTagData(BaseUtil.getHexByteArray(CURRENT_EPC), btPassword, bank, address, length, finalBuffer);
                if (isdatawritten) {
                    NEW_EPC = CURRENT_EPC.substring(0,2)+str_data+CURRENT_EPC.substring(4);
                    if (cd.isConnectingToInternet()) {
                        try {
                            hideProgressDialog();
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put(APIConstants.K_ASSET_ID,assetid);
                            jsonObject.put(APIConstants.K_UID, SharedPreferencesManager.getSavedUserId(context));
                            //jsonObject.put(APIConstants.K_ASSET_SERIAL_NUMBER,serialnumber);
                            jsonObject.put(APIConstants.K_ASSET_SERIAL_NUMBER,AssetUtils.hexToNumber(serialnumber));
                            jsonObject.put(APIConstants.K_CUSTOMER_ID,SharedPreferencesManager.getCustomerId(context));
                            jsonObject.put(APIConstants.K_CURRENT_TAG_ID,NEW_EPC);
                            jsonObject.put(APIConstants.K_PREVIOUS_TAG_ID,CURRENT_EPC);
                            jsonObject.put(APIConstants.K_TAG_TID,CURRENT_TID);
                            doRegistration(jsonObject, APIConstants.M_ASSET_REGISTRATION, "Please wait...\n" + "Asset-RFID mapping is in progress",assetid,serialnumber);
                        } catch (JSONException e) {
                            hideProgressDialog();
                            allow_trigger_to_press = true;
                        }

                    } else {
                        hideProgressDialog();
                        allow_trigger_to_press = true;
                        AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                    }
                } else {
                    if(writecount==1){
                        writeTagData(serialnumber);
                    }else {
                        setDefault();
                        AssetUtils.showCommonBottomSheetErrorDialog(context, "Asset-RFID Mapping Failed please try again (Tag writing failed)");
                    }
                }
            }
        }, 100);

    }

    public void doValidationsAfterMapping(String assetid,String serialnumber){
        AssetUtils.showCommonBottomSheetSuccessDialog(context,"Asset-RFID mapping done successfully");
        //TODO call here api to upload data
        db.updateAssetAsRegistered(assetid,serialnumber);
        if (eList != null) {
            eList.clear();
        }
        eList = db.getAllNonRegisteredAssetList();
        notifyAdapter();
        setDefault();
    }
    public void setDefault() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CURRENT_TID = "";
                CURRENT_EPC = "";
                NEW_EPC = "";
                binding.imgStatus.setImageDrawable(getDrawable(R.drawable.rfidscan));
                binding.edtAssetType.setText("");
                binding.edtRfidNumber.setText("");
                SELECTED_ASSET = "";
                binding.spAsset.setSelectedItem(0);
                allow_trigger_to_press = true;
            }
        });

    }

    public void changeImageStatusToRfidScan() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideProgressDialog();
                CURRENT_TID = "";
                CURRENT_EPC = "";
                NEW_EPC = "";
                binding.imgStatus.setImageDrawable(getDrawable(R.drawable.rfidscan));
                allow_trigger_to_press = true;
                BeepClass.errorbeep(context);
            }
        });

    }

    private void notifyAdapter() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideProgressDialog();
                binding.spAsset.setEnabled(false);
                assetMappingSearchAdapter = new AssetMappingSearchAdapter(context, eList);
                binding.spAsset.setAdapter(assetMappingSearchAdapter);
                binding.spAsset.setEnabled(true);
            }
        });

    }


    @Override
    public void onResume() {
        super.onResume();
        rfidHandler.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        rfidHandler.onDestroy();
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


    public void doRegistration(final JSONObject loginRequestObject, String METHOD_NAME, String progress_message,String assetid,String serialnumber) {
        showProgress(context, progress_message);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();

        Log.e("REGISTRATIONURL",SharedPreferencesManager.getHostUrl(context)+METHOD_NAME);
        Log.e("REGISTRATIONREQUEST",loginRequestObject.toString());
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
                                Log.e("REGISTRATIONRESULT",result.toString());
                                String status = result.getString(APIConstants.K_STATUS);
                                String message = result.getString(APIConstants.K_MESSAGE);

                                if (status.equalsIgnoreCase("true")) {
                                    doValidationsAfterMapping(assetid,serialnumber);
                                    setDefault();
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
                        Log.e("ERROR", anError.getErrorDetail());
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

}