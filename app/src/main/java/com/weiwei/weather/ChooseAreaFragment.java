package com.weiwei.weather;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.weiwei.weather.db.City;
import com.weiwei.weather.db.County;
import com.weiwei.weather.db.Province;
import com.weiwei.weather.util.HttpUtil;
import com.weiwei.weather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    private static final int LEVEL_PROVINCE = 0;
    private static final int LEVEL_CITY = 1;
    private static final int LEVEL_COUNT = 2;
    private ProgressDialog progressDialog;
    private TextView titleTest;
    private Button buttonBack;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> datalist = new ArrayList<String>();
    private List<City> cityList;
    private List<County> countyList;
    private List<Province> provinceList;
    private Province selectedProvinced;
    private City selectedCity;
    private int currentlevel;
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle savadInstancesState) {
        View view = layoutInflater.inflate(R.layout.choose_area,viewGroup,false);
        titleTest = (TextView) view.findViewById(R.id.title_text);
        buttonBack = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,datalist);
        listView.setAdapter(adapter);
        return view;
    }
    public void onActivityCreated(Bundle savadInstanceState) {//这个方法没有完全的理解
        super.onActivityCreated(savadInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if(currentlevel==LEVEL_PROVINCE){
                     selectedProvinced = provinceList.get(position);
                     queryCities();
                }else if(currentlevel == LEVEL_CITY){
                     selectedCity = cityList.get(position);
                     queryCouties();
                 }
            }
        });
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentlevel == LEVEL_COUNT){
                    queryCities();
                }else  if(currentlevel ==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }
    //省份查询
    public void queryProvinces(){
        titleTest.setText("中国");
        buttonBack.setVisibility(View.GONE);//设置按钮不可见
        provinceList = DataSupport.findAll(Province.class);//LitaPal的查询 但是我我不知道为什么里面的参数是这样设置的
        if(provinceList.size()>0){
            datalist.clear();
            for(Province province:provinceList){
                datalist.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentlevel=LEVEL_PROVINCE;
        }else{
            //去申请网络请求去和风天气查询
            String address = "http://guolin.tech/api/china/";
            queryFromServer(address,"province");
        }
    }
    //查询市
    public void queryCities(){
        titleTest.setText(selectedProvinced.getProvinceName());
        buttonBack.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvinced.getId())).find(City.class);
        if(cityList.size() >0 ){
            datalist.clear();
            for(City city:cityList){
                datalist.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);//似乎没什么作用，也可能是不起作用
            currentlevel = LEVEL_CITY;
        }else{
            //去申请数据去和风天气查询
            int provinceCode = selectedProvinced.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }
    }
    //县查询
    public void queryCouties(){
        titleTest.setText(selectedCity.getCityName());
        buttonBack.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.
                getId())).find(County.class);
        if(countyList.size()>0){
            datalist.clear();
            for(County county :countyList){
                datalist.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();//数据刷新
            listView.setSelection(0);//似乎没什么作用，也可能是不起作用
            currentlevel = LEVEL_COUNT;
        }else{
            //去数据库查询
            int provinceCode = selectedProvinced.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"county");
        }
    }
    //按照地址以及类型 从服务器上面查询数据
    public void queryFromServer(String address,final String type){
        showProgressDialog();//显示查询进度条
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String onResponsetext = response.body().string();//这里不明白是什么意思
                boolean result = false;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(onResponsetext);
                }else if("city".equals(type)) {
                    result = Utility.handleCityResponse(onResponsetext,selectedProvinced.getId());
                }else if("county".equals(type)){
                    result = Utility.handleCountyResponse(onResponsetext,selectedCity.getId());
                }
                if(result){
                    //这下面的我不怎么明白
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();//关闭加载的那个东西
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCouties();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    public void showProgressDialog(){
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(true);//这里为什么设置这个来着 我给忘了
            //可能的解释 点击外部位置无法取消这个加载的dialog
        }
        progressDialog.show();
    }
    public void closeProgressDialog(){
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }
}
