package de.chagemann.darfichkiffen;

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module;
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
class UtilsModule {

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            encodeDefaults = true // for requests we want default values to be included
            isLenient = true // quoted booleans and unquoted strings are allowed
            coerceInputValues = true // coerce unknown enum values to default
            explicitNulls = false
        }
    }

    @Provides
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
}