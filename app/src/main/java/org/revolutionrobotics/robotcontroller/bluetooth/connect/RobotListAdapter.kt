package org.revolutionrobotics.robotcontroller.bluetooth.connect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.revolutionrobotics.bluetooth.android.domain.Device
import org.revolutionrobotics.robotcontroller.bluetooth.R.id
import org.revolutionrobotics.robotcontroller.bluetooth.R.layout


class RobotListAdapter(
    var onRobotSelected: ((Device) -> Unit)? = null
) : RecyclerView.Adapter<RobotListAdapter.MyViewHolder>() {

    private val robots = mutableListOf<Device>()
    private var deviceConnectingTo: Device? = null



    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        var name: TextView = itemView.findViewById(id.robot_name)
        var progress: ProgressBar = itemView.findViewById(id.progress)

    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        // create a new view
        val view = LayoutInflater.from(parent.context)
            .inflate(layout.item_robot, parent, false)
        return MyViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.name.text = robots[position].name
        holder.progress.visibility = if (deviceConnectingTo == robots[position]) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { v ->
            onRobotSelected?.invoke(robots[position])
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = robots.size

    fun showConnecting(device: Device) {
        deviceConnectingTo = device
        notifyDataSetChanged()
    }

    fun addItems(robots: List<Device>) {
        this.robots.addAll(robots.filter { robot -> !this.robots.contains(robot) } )
        notifyDataSetChanged()
    }
}