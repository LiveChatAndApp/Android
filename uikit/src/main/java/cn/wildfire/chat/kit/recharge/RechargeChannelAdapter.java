package cn.wildfire.chat.kit.recharge;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.PaymentInfo;
import cn.wildfirechat.model.RechargeChannel;

public class RechargeChannelAdapter extends BaseAdapter {
    private List<RechargeChannel> rechargeChannelList = new ArrayList<>();

    public RechargeChannelAdapter() {

    }

    public void setData(List<RechargeChannel> list) {
        if (list == null)
            return;
        rechargeChannelList.clear();
        rechargeChannelList.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return rechargeChannelList.size();
    }

    @Override
    public Object getItem(int position) {
        return rechargeChannelList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_select_bank, null);
            viewHolder = new ViewHolder();
            viewHolder.text = (MaterialTextView) convertView.findViewById(R.id.text);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        try {
            if (position < 0) {
                position = 0;
            }
            RechargeChannel info = rechargeChannelList.get(position);
            viewHolder.text.setText(info.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return convertView;
    }

    class ViewHolder {
        MaterialTextView text;
    }
}
