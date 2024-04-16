package com.psl.pallettracking;

import static com.psl.pallettracking.helper.AssetUtils.hideProgressDialog;
import static com.psl.pallettracking.helper.AssetUtils.showProgress;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.BuildConfig;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.psl.pallettracking.adapters.DashboardModel;
import com.psl.pallettracking.database.AssetMaster;
import com.psl.pallettracking.database.DatabaseHandler;
import com.psl.pallettracking.databinding.ActivityLoginBinding;
import com.psl.pallettracking.helper.APIConstants;
import com.psl.pallettracking.helper.AppConstants;
import com.psl.pallettracking.helper.AssetUtils;
import com.psl.pallettracking.helper.ConnectionDetector;
import com.psl.pallettracking.helper.SharedPreferencesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private Context context = this;
    private DatabaseHandler db;
    private ConnectionDetector cd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        setTitle("USER LOGIN");
        getSupportActionBar().hide();

        cd = new ConnectionDetector(context);
        db = new DatabaseHandler(context);
       /* AssetUtils.getUTCSystemDateTimeInFormatt();
        String dt = AssetUtils.getSystemDateTimeInFormatt();
        Log.e("NORMALDT",dt);*/


        String androidID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        androidID = androidID.toUpperCase();
        SharedPreferencesManager.setDeviceId(context, androidID);
        Log.e("DEVICEID", androidID);

        if (SharedPreferencesManager.getIsHostConfig(context)) {

        } else {
            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.url_not_config));
        }

        //TODO comment below
        /*Intent i = new Intent(LoginActivity.this,PalletMovementActivity.class);
        i.putExtra("WorkOrderNumber","bdjhsb");
        i.putExtra("WorkOrderType","L0");
        startActivity(i);*/

        if (SharedPreferencesManager.getIsLoginSaved(context)) {
            binding.chkRemember.setChecked(true);
            binding.edtUserName.setText(SharedPreferencesManager.getSavedUser(context));
            binding.edtPassword.setText(SharedPreferencesManager.getSavedPassword(context));
        } else {
            binding.chkRemember.setChecked(false);
            binding.edtUserName.setText("");
            binding.edtPassword.setText("");
        }

        binding.btnLogin.setOnClickListener(view -> {
           /* Intent loginIntent = new Intent(LoginActivity.this, DashboardActivity.class);
            startActivity(loginIntent);*/
            if (SharedPreferencesManager.getIsHostConfig(context)) {

                String user = binding.edtUserName.getText().toString().trim();
                String password = binding.edtPassword.getText().toString().trim();
               /* try {
                    password = PSLEncryption.encrypt(password,PSLEncryption.publicKey);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                }*/
                if (user.equalsIgnoreCase("") || password.equalsIgnoreCase("")) {
                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.login_data_validation));
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(APIConstants.K_USER, user);
                        jsonObject.put(APIConstants.K_PASSWORD, password);
                        jsonObject.put(APIConstants.K_DEVICE_ID, SharedPreferencesManager.getDeviceId(context));
                        userLogin(jsonObject, APIConstants.M_USER_LOGIN, "Please wait...\n" + "User login is in progress");

                       /* Intent loginIntent = new Intent(LoginActivity.this, DashboardActivity.class);
                        startActivity(loginIntent);*/
                    } catch (JSONException e) {

                    }
                }
            } else {
                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.url_not_config));
            }

        });

        binding.imgSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent configIntent = new Intent(LoginActivity.this, URLConfigActivity.class);
                startActivity(configIntent);

            }
        });
        binding.btnClear.setOnClickListener(view -> {
            binding.chkRemember.setChecked(false);
            binding.edtUserName.setText("");
            binding.edtPassword.setText("");
            SharedPreferencesManager.setIsLoginSaved(context, false);
            SharedPreferencesManager.setSavedUser(context, "");
            SharedPreferencesManager.setSavedPassword(context, "");
            binding.chkRemember.setChecked(false);
        });
        binding.textDeviceId.setText("Share device ID to admin for device registration\nDevice ID: " + SharedPreferencesManager.getDeviceId(context) + "\nIgnore if device already registered.");
    }


    public void userLogin(final JSONObject loginRequestObject, String METHOD_NAME, String progress_message) {
        showProgress(context, progress_message);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(APIConstants.API_TIMEOUT, TimeUnit.SECONDS)
                .build();

        Log.e("URL", SharedPreferencesManager.getHostUrl(context) + METHOD_NAME);
        Log.e("LOGINREQUEST", loginRequestObject.toString());
        AndroidNetworking.post(SharedPreferencesManager.getHostUrl(context) + METHOD_NAME).addJSONObjectBody(loginRequestObject)
                .setTag("test")
                .setPriority(Priority.LOW)
                .setOkHttpClient(okHttpClient) // passing a custom okHttpClient
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject result) {
                        hideProgressDialog();
                        parseJson(result, loginRequestObject);
                    }

                    @Override
                    public void onError(ANError anError) {
                        hideProgressDialog();
                        Log.e("ERROR", anError.getErrorDetail());
//                        if (BuildConfig.DEBUG) {
//                            // do something for a debug build
//                            try {
//                                parseJson(new JSONObject(AssetUtils.getJsonFromAssets(context,"loginres.json")),new JSONObject(AssetUtils.getJsonFromAssets(context,"loginreq.json")));
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
//                        }else{
                        if (anError.getErrorDetail().equalsIgnoreCase("responseFromServerError")) {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
                        } else if (anError.getErrorDetail().equalsIgnoreCase("connectionError")) {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                        } else {
                            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.internet_error));
                        }
                         //}

                    }
                });
    }

    private void parseJson(JSONObject result, JSONObject loginRequestObject) {
        if (result != null) {
            try {
                Log.e("LOGINRESULT", result.toString());
                String status = result.getString(APIConstants.K_STATUS).trim();
                String message = result.getString(APIConstants.K_MESSAGE).trim();

                if (status.equalsIgnoreCase("true")) {
                    SharedPreferencesManager.setSavedUser(context, loginRequestObject.getString(APIConstants.K_USER));
                    SharedPreferencesManager.setSavedPassword(context, loginRequestObject.getString(APIConstants.K_PASSWORD));
                    SharedPreferencesManager.setSavedPassword(context, binding.edtPassword.getText().toString().trim());
                    JSONObject dataObject = null;
                    if (result.has(APIConstants.K_DATA)) {
                        dataObject = result.getJSONObject(APIConstants.K_DATA);
                        if (dataObject != null) {
                            if (dataObject.has(APIConstants.K_CUSTOMER_ID)) {
                                String customerid = dataObject.getString(APIConstants.K_CUSTOMER_ID).trim();
                                SharedPreferencesManager.setCustomerId(context, customerid);
                            }
                            if (dataObject.has(APIConstants.K_TAG_ACCESS_PASSWORD)) {
                                String tagpassword = dataObject.getString(APIConstants.K_TAG_ACCESS_PASSWORD).trim();
                                               /* try {
                                                    tagpassword = PSLEncryption.decrypt(tagpassword,PSLEncryption.publicKey);
                                                } catch (GeneralSecurityException e) {
                                                    e.printStackTrace();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }*/
                                tagpassword = AssetUtils.numberToHex(tagpassword);
                                tagpassword = AssetUtils.get8DigitAssetSerialNumber(tagpassword);
                                if (tagpassword.length() > 8) {
                                    SharedPreferencesManager.setCurrentAccessPassword(context, tagpassword);
                                }
                            }

                            if (dataObject.has(APIConstants.K_USER_ID)) {
                                String userid = dataObject.getString(APIConstants.K_USER_ID).trim();
                                SharedPreferencesManager.setSavedUserId(context, userid);
                            }

                            if (dataObject.has(APIConstants.K_COMPANY_CODE)) {
                                String companycode = dataObject.getString(APIConstants.K_COMPANY_CODE).trim();
                                companycode = AssetUtils.numberToHex(companycode);
                                Log.e("COMPANYCODE", companycode);
                                SharedPreferencesManager.setCompanyCode(context, companycode);
                            }

                            List<AssetMaster> assetTypeMasterList = new ArrayList<>();
                            List<DashboardModel> dashboardMenuList = new ArrayList<>();

                            if (dataObject.has(APIConstants.K_DASHBOARD_ARRAY)) {
                                JSONArray dashboardMenu = null;
                                dashboardMenu = dataObject.getJSONArray(APIConstants.K_DASHBOARD_ARRAY);
                                if (dashboardMenu.length() > 0) {
                                    boolean syncAvailable = false;
                                    for (int dashboard = 0; dashboard < dashboardMenu.length(); dashboard++) {
                                        DashboardModel dashboardModel = new DashboardModel();
                                        JSONObject dashboardObject = dashboardMenu.getJSONObject(dashboard);
                                        if (dashboardObject.has(APIConstants.K_DASHBOARD_MENU_ID)) {
                                            String menuid = dashboardObject.getString(APIConstants.K_DASHBOARD_MENU_ID).trim();
                                            menuid = menuid.trim();
                                            dashboardModel.setMenuId(menuid);

                                            Log.e("MENUID", menuid + " ORIGINAL:" + AppConstants.MENU_ID_ASSETSYNC + " ==>" + menuid.equalsIgnoreCase(AppConstants.MENU_ID_ASSETSYNC));
                                            if (menuid.equalsIgnoreCase(AppConstants.MENU_ID_ASSETSYNC)) {
                                                Log.e("MENUIDTRUE", menuid);
                                                syncAvailable = true;
                                            }

                                        }
                                        if (dashboardObject.has(APIConstants.K_DASHBOARD_MENU_NAME)) {
                                            dashboardModel.setMenuName(dashboardObject.getString(APIConstants.K_DASHBOARD_MENU_NAME).trim());
                                        }
                                        if (dashboardObject.has(APIConstants.K_DASHBOARD_MENU_ACTIVITY_NAME)) {
                                            dashboardModel.setMenuActivityName(dashboardObject.getString(APIConstants.K_DASHBOARD_MENU_ACTIVITY_NAME).trim());
                                        }
                                        if (dashboardObject.has(APIConstants.K_DASHBOARD_MENU_SEQUENCE)) {
                                            dashboardModel.setMenuSequence(dashboardObject.getString(APIConstants.K_DASHBOARD_MENU_SEQUENCE).trim());
                                        }
                                        if (dashboardObject.has(APIConstants.K_DASHBOARD_MENU_ACTIVE)) {
                                            dashboardModel.setIsMenuActive(dashboardObject.getString(APIConstants.K_DASHBOARD_MENU_ACTIVE).trim());
                                        }
                                        if (dashboardObject.has(APIConstants.K_DASHBOARD_MENU_IMAGE)) {
                                            dashboardModel.setMenuimageName(dashboardObject.getString(APIConstants.K_DASHBOARD_MENU_IMAGE).trim());
                                        }
                                        dashboardMenuList.add(dashboardModel);
                                    }

                                    if (!syncAvailable) {
                                        DashboardModel dashboardModelSync = new DashboardModel();
                                        dashboardModelSync.setMenuName(AppConstants.DASHBOARD_MENU_SYNC);
                                        dashboardModelSync.setDrawableImage(R.mipmap.ic_launcher_sync_foreground);
                                        dashboardModelSync.setMenuId(AppConstants.MENU_ID_ASSETSYNC);
                                        dashboardModelSync.setMenuimageName("");
                                        dashboardModelSync.setMenuSequence("5");
                                        dashboardModelSync.setIsMenuActive("true");
                                        dashboardMenuList.add(dashboardModelSync);
                                    }
                                }

                            } else {

                            }

                            if (dataObject.has(APIConstants.K_ASSETTYPE_MASTER)) {
                                JSONArray assettypeMaster = null;
                                assettypeMaster = dataObject.getJSONArray(APIConstants.K_ASSETTYPE_MASTER);

                                if (assettypeMaster.length() > 0) {
                                    for (int assettype = 0; assettype < assettypeMaster.length(); assettype++) {
                                        AssetMaster assetMaster = new AssetMaster();
                                        JSONObject vendorObject = assettypeMaster.getJSONObject(assettype);
                                        if (vendorObject.has(APIConstants.K_ASSET_TYPE_ID)) {
                                            String assettypeid = vendorObject.getString(APIConstants.K_ASSET_TYPE_ID).trim();
                                            assettypeid = AssetUtils.numberToHex(assettypeid);
                                            assettypeid = AssetUtils.get2DigitAssetTypeId(assettypeid);
                                            assetMaster.setAssetTypeId(assettypeid);
                                        }

                                        if (vendorObject.has(APIConstants.K_ASSET_TYPE_NAME)) {
                                            assetMaster.setAssetTypeName(vendorObject.getString(APIConstants.K_ASSET_TYPE_NAME).trim());
                                        }
                                        assetTypeMasterList.add(assetMaster);
                                    }
                                }

                            }

                            if (assetTypeMasterList.size() > 0) {

                                if (dashboardMenuList.size() > 0) {
                                    db.deleteDashboardMenuMaster();
                                    db.storeDashboardMenuMaster(dashboardMenuList);
                                } else {
                                    db.deleteDashboardMenuMaster();
                                    List<DashboardModel> dashboardList = AssetUtils.getDashboardDetails();
                                    db.storeDashboardMenuMaster(dashboardList);
                                }

                                db.deleteAssetTypeMaster();
                                db.storeAssetTypeMaster(assetTypeMasterList);

                                if (binding.chkRemember.isChecked()) {
                                    SharedPreferencesManager.setIsLoginSaved(context, true);
                                } else {
                                    SharedPreferencesManager.setIsLoginSaved(context, false);
                                }

                                if (db.getDashboardMenuCount() > 0) {
                                    Intent loginIntent = new Intent(LoginActivity.this, DashboardActivity.class);
                                    startActivity(loginIntent);
                                } else {
                                    AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.no_dashboard_menu_active));
                                }

                            } else {
                                AssetUtils.showCommonBottomSheetErrorDialog(context, "Asset Type/Vendor Master is Empty");
                            }
                        }
                    }

                } else {
                    AssetUtils.showCommonBottomSheetErrorDialog(context, message);
                }
            } catch (JSONException e) {
                hideProgressDialog();
                AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.something_went_wrong_error));
            }
        } else {
            hideProgressDialog();
            AssetUtils.showCommonBottomSheetErrorDialog(context, getResources().getString(R.string.communication_error));
        }
    }


}