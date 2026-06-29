import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FrontofficeComponent } from './frontoffice.component';
import { HomeComponent } from './home/home.component';
import { CvAnalyseComponent } from './cv-analyse/cv-analyse.component';
import { JobMatchComponent } from './job-match/job-match.component';
import { ProfilComponent } from './profil/profil.component';
import { PlanCarriereComponent } from './plan-carriere/plan-carriere.component';
import { InterviewPreparationComponent } from './interview-preparation/interview-preparation.component';
import { InterviewSessionComponent } from './interview-session/interview-session.component';
import { InterviewFeedbackComponent } from './interview-feedback/interview-feedback.component';

const routes: Routes = [
  {
    path: '',
    component: FrontofficeComponent,
    children: [
      { path: '',          redirectTo: 'home', pathMatch: 'full' },
      { path: 'home',      component: HomeComponent },
      { path: 'cvAnalyse', component: CvAnalyseComponent },
      { path: 'jobMatching', component: JobMatchComponent },
      { path: 'interviewPrep', component: InterviewPreparationComponent },
      { path: 'careerRoadmap', component: PlanCarriereComponent },
      { path: 'plan-carriere', redirectTo: 'careerRoadmap', pathMatch: 'full' },
      { path: 'profil', component: ProfilComponent },
      { path: 'interview-session',  component: InterviewSessionComponent  },
      { path: 'interview-feedback', component: InterviewFeedbackComponent },
    ] 
    
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FrontofficeRoutingModule { }
