package com.example.sitim

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class productAdapter (private val prodList: ArrayList<String>) : RecyclerView.Adapter<productAdapter.myViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.products_indi, parent, false)
        return myViewHolder(itemView)
    }

    class myViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val pname: TextView = itemView.findViewById(R.id.indi_product_name)
        val pqty: TextView = itemView.findViewById(R.id.qty_indi)
    }

    override fun onBindViewHolder(holder: myViewHolder, position: Int) {
        val det = prodList[position]
        val details = det.split(",").toTypedArray()

        holder.pname.text = details[0]
        holder.pqty.text = details[1]
    }

    override fun getItemCount(): Int {
        return prodList.size
    }
}