package com.example.agatepedia.ui

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.agatepedia.di.Injection
import com.example.agatepedia.data.remote.AgateRepository
import com.example.agatepedia.ui.bookmark.BookmarkViewModel
import com.example.agatepedia.ui.detailagatepedia.DetailViewModel
import com.example.agatepedia.ui.home.HomeViewModel
import java.lang.IllegalArgumentException

class ViewModelFactory private constructor(private val agateRepository: AgateRepository):
    ViewModelProvider.NewInstanceFactory(){
        @Suppress("UNCHECKED_CAST")
        override fun <T: ViewModel> create(modelClass: Class<T>): T{
            if(modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(agateRepository) as T
            } else if( modelClass.isAssignableFrom(DetailViewModel::class.java)){
                return DetailViewModel(agateRepository) as T
            }else if( modelClass.isAssignableFrom(BookmarkViewModel::class.java)){
                return BookmarkViewModel(agateRepository) as T
            }
            throw IllegalArgumentException("Unknown Viewmodel class: " + modelClass.name)
        }


    companion object{
        @Volatile
        private var instance: ViewModelFactory? = null
        fun getInstance(context: Context): ViewModelFactory =
            instance?: synchronized(this){
                instance?: ViewModelFactory(Injection.provideRepository(context))
            }.also { instance = it }
    }
}