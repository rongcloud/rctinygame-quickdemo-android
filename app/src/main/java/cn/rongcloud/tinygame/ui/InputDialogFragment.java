package cn.rongcloud.tinygame.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import cn.rongcloud.tinygame.R;

/**
 * @author gyn
 * @date 2022/3/7
 */
public class InputDialogFragment extends DialogFragment {
    private static final String TAG = InputDialogFragment.class.getSimpleName();
    OnButtonClickListener onButtonClickListener;

    public InputDialogFragment(OnButtonClickListener onButtonClickListener) {
        this.onButtonClickListener = onButtonClickListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getContext()).inflate(R.layout.fragment_input_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText editText = view.findViewById(R.id.et_text);
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            if (onButtonClickListener != null) {
                String text = editText.getText().toString().trim();
                onButtonClickListener.clickConfirm(text);
                dismiss();
            }
        });
    }

    public void show(FragmentManager fragmentManager) {
        super.show(fragmentManager, TAG);
    }

    public interface OnButtonClickListener {
        void clickConfirm(String text);
    }
}
