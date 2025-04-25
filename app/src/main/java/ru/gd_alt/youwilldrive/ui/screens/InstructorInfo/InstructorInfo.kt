package ru.gd_alt.youwilldrive.ui.screens.InstructorInfo


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.models.Car
import ru.gd_alt.youwilldrive.models.Instructor
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultCar1
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultCar2
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultCar3
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultCar4
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultInstructor
import ru.gd_alt.youwilldrive.ui.screens.Profile.LoadingCard

@Preview(showBackground = true)
@Composable
fun InstructorInfo(
    instructor: Instructor = DefaultInstructor,
    viewModel: InstructorInfoViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var cars: List<Car>? by remember { mutableStateOf(null) }
    LaunchedEffect(scope) {
        viewModel.fetchCars(instructor) { result, error ->
            cars = result
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (viewModel.instructorInfoState.collectAsState().value == InstructorInfoState.Loading) {
            LoadingCard()
        }
        else {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Cars(cars ?: emptyList())
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.cadets),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun Cars(
    cars: List<Car> = listOf(DefaultCar1, DefaultCar2, DefaultCar3, DefaultCar4)
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        for (i in 0..cars.size / 2) {
            Row(
                Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (car in cars.slice(i*2..kotlin.math.min(i*2+1, cars.size-1))) {
                    Card(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1.5f).padding(5.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                    ) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                Modifier
                                    .padding(5.dp).fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Absolute.Left
                            ) {
                                Icon(Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text="${car.model}, ${car.color}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Text(text=car.plateNumber)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}