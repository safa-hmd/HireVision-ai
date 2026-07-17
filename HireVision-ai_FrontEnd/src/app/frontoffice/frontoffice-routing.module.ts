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
import { SubscriptionComponent } from './subscription/subscription.component';
import { LandingComponent } from './landing/landing.component';
import { AuthGuard } from '../guards/auth.guard';

const routes: Routes = [
  {
    path: '',
    component: FrontofficeComponent,
    children: [
      // Première page : page welcome/landing (accessible sans connexion)
      { path: '',          redirectTo: 'welcome', pathMatch: 'full' },
      { path: 'welcome',   component: LandingComponent },

      // Pages protégées : connexion requise
      { path: 'home',            component: HomeComponent,                canActivate: [AuthGuard] },
      { path: 'cvAnalyse',       component: CvAnalyseComponent,           canActivate: [AuthGuard] },
      { path: 'jobMatching',     component: JobMatchComponent,             canActivate: [AuthGuard] },
      { path: 'interviewPrep',   component: InterviewPreparationComponent, canActivate: [AuthGuard] },
      { path: 'careerRoadmap',   component: PlanCarriereComponent,         canActivate: [AuthGuard] },
      { path: 'profil',          component: ProfilComponent,               canActivate: [AuthGuard] },
      { path: 'interview-session',  component: InterviewSessionComponent,  canActivate: [AuthGuard] },
      { path: 'interview-feedback', component: InterviewFeedbackComponent, canActivate: [AuthGuard] },
      { path: 'subscription',    component: SubscriptionComponent,         canActivate: [AuthGuard] },

      // Alias maintenu pour compatibilité
      { path: 'plan-carriere', redirectTo: 'careerRoadmap', pathMatch: 'full' },
    ]
  }
];


@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FrontofficeRoutingModule { }
