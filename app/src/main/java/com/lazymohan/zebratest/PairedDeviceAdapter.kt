package com.lazymohan.zebratest

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lazymohan.zebratest.databinding.ItemPairedDeviceBinding

class PairedDeviceAdapter(
  private val arrayList: MutableList<PairedDeviceModel>,
  val listener: DeviceItemListener
) : RecyclerView.Adapter<PairedDeviceAdapter.ViewHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val view = ItemPairedDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    holder.setDevice(arrayList[position])
  }

  override fun getItemCount(): Int = arrayList.size

  inner class ViewHolder(private val binding: ItemPairedDeviceBinding) : RecyclerView.ViewHolder(
      binding.root
  ) {
    fun setDevice(item: PairedDeviceModel) {
      binding.friendlyName.text = item.friendlyName
      binding.root.setOnClickListener {
        onItemClicked()
      }
    }

    private fun onItemClicked() {
      val position = adapterPosition
      val model = arrayList[position]
      listener.getDevice(model)
    }
  }

  interface DeviceItemListener {
    fun getDevice(model: PairedDeviceModel)
  }
}