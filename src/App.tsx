import Nav from "./components/Nav";
import Hero from "./components/Hero";
import Features from "./components/Features";
import Architecture from "./components/Architecture";
import ApiSetup from "./components/ApiSetup";
import BuildSteps from "./components/BuildSteps";
import Tests from "./components/Tests";
import ProjectTree from "./components/ProjectTree";
import Limits from "./components/Limits";
import Footer from "./components/Footer";

export default function App() {
  return (
    <div className="min-h-screen bg-ink-950 text-slate-300">
      <Nav />
      <main>
        <Hero />
        <Features />
        <Architecture />
        <ApiSetup />
        <BuildSteps />
        <Tests />
        <ProjectTree />
        <Limits />
      </main>
      <Footer />
    </div>
  );
}
