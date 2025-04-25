package ru.gd_alt.youwilldrive.ui.screens.CadetInfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.gd_alt.youwilldrive.R
import ru.gd_alt.youwilldrive.models.Cadet
import ru.gd_alt.youwilldrive.models.Placeholders.DefaultCadet
import ru.gd_alt.youwilldrive.models.Plan
import ru.gd_alt.youwilldrive.ui.screens.Profile.LoadingCard


@Composable
fun CadetInfo(
    cadet: Cadet = DefaultCadet,
    viewModel: CadetInfoViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var plan: Plan? by remember { mutableStateOf(null) }

    LaunchedEffect(scope) {
        viewModel.fetchPlan(cadet) { data, _ ->
            plan = data
        }
    }

    if (viewModel.cadetInfoState.collectAsState().value == CadetInfoState.Loading) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.5f)) {
            LoadingCard()
        }
        return
    }

    CadetInfoRows(
        planName = plan?.name ?: stringResource(R.string.plan),
        practiceHours = cadet.hoursAlready,
        totalPractice = plan?.practiceHours ?: 1
    )
}

@Composable
fun CadetInfoRows(
    planName: String = "Тариф",
    practiceHours: Int = 25,
    totalPractice: Int = 50
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.plan),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    planName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                stringResource(R.string.practice_hours),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$practiceHours", style = MaterialTheme.typography.bodyMedium)
                Text("$totalPractice", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { practiceHours.toFloat() / totalPractice.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}