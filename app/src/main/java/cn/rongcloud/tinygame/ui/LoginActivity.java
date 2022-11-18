package cn.rongcloud.tinygame.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import cn.rongcloud.tinygame.BuildConfig;
import cn.rongcloud.tinygame.GameConstant;
import cn.rongcloud.tinygame.R;
import cn.rongcloud.tinygame.model.User;
import cn.rongcloud.tinygame.model.UserManager;
import cn.rongcloud.tinygame.oklib.OkApi;
import cn.rongcloud.tinygame.oklib.OkParams;
import cn.rongcloud.tinygame.oklib.WrapperCallBack;
import cn.rongcloud.tinygame.oklib.wrapper.OkHelper;
import cn.rongcloud.tinygame.oklib.wrapper.Wrapper;
import cn.rongcloud.tinygame.oklib.wrapper.interfaces.IHeader;
import cn.rongcloud.tinygame.utils.DeviceUtils;
import cn.rongcloud.tinygame.utils.ToastUtil;
import io.rong.imkit.IMCenter;
import io.rong.imkit.picture.tools.ToastUtils;
import okhttp3.Headers;

/**
 * @author gyn
 * @date 2022/3/4
 */
public class LoginActivity extends AppCompatActivity {
    private TextView tvDesc;
    private EditText etPhone;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        addBusinessToken();
        initView();
    }

    private void addBusinessToken() {
        OkHelper.get().setHeadCacher(new IHeader() {
            @Override
            public Map<String, String> onAddHeader() {
                Map<String, String> map = new HashMap<String, String>();
                map.put("BusinessToken", BuildConfig.BUSINESS_TOKEN);
                return map;
            }

            @Override
            public void onCacheHeader(Headers headers) {
            }

        });
    }

    private void initView() {
        tvDesc = (TextView) findViewById(R.id.tv_desc);
        etPhone = (EditText) findViewById(R.id.et_phone);
        if (UserManager.getUser() != null) {
            MainActivity.show(this);
            finish();
        } else {

        }
    }


    public void clickLogin(View view) {
        if (etPhone.getText() == null) {
            ToastUtils.s(this, "请输入11位手机号码");
            return;
        }
        String phone = etPhone.getText().toString().trim();
        if (phone.length() != 11) {
            ToastUtils.s(this, "请输入11位手机号码");
            return;
        }

        Map<String, Object> params = OkParams.get()
                .add("mobile", phone)
                .add("verifyCode", "123456")
                .add("deviceId", DeviceUtils.getDeviceId()).build();
        OkApi.post(GameConstant.LOGIN_URL, params, new WrapperCallBack() {

            @Override
            public void onResult(Wrapper result) {
                if (result.ok()) {
                    User user = result.get(User.class);
                    UserManager.setUser(user);
                    IMCenter.getInstance().connect(user.getImToken(), null);
                    MainActivity.show(LoginActivity.this);
                    finish();
                } else {
                    ToastUtil.show(result.getMessage());
                }
            }

            @Override
            public void onError(int code, String msg) {
                super.onError(code, msg);
                ToastUtil.show(msg);
            }
        });
    }

}
