package com.dynamicdudes.realar.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.dynamicdudes.realar.Data.Model
import com.dynamicdudes.realar.R
import kotlinx.android.synthetic.main.item_model.view.*

const val SELECTED_BACKGROUND_COLOR = R.color.endViolet
const val UNSELECTED_BACKGROUND_COLOR = Color.LTGRAY


class ModelAdapter(val models : List<Model>) : RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {


    var selectedModel = MutableLiveData<Model>()
    private var selectedModelIndex = 0
    private var modelViewHolder = mutableListOf<ModelViewHolder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_model,parent,false)
        return ModelViewHolder(view)
    }

    override fun getItemCount() = models.size

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        if(!modelViewHolder.contains(holder)){
            modelViewHolder.add(holder)
        }
        holder.itemView.apply{
            image_view.setImageResource(models[position].imageResourceId)
            name_text_view.text = models[position].title
            if(selectedModelIndex == position){
                setBackgroundColor(SELECTED_BACKGROUND_COLOR)
                selectedModel.value = models[position]
            }
            setOnClickListener {
                selectItem(position)
            }
        }
    }

    private fun selectItem(position : Int){
        modelViewHolder[selectedModelIndex].itemView.setBackgroundColor(UNSELECTED_BACKGROUND_COLOR)
        selectedModelIndex = position
        modelViewHolder[selectedModelIndex].itemView.setBackgroundColor(SELECTED_BACKGROUND_COLOR)
        selectedModel.value = models[position]
    }

    inner class ModelViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView)


}